package com.aqe.engine;

import com.aqe.component.MarketDataCmp;
import com.aqe.context.StrategyContext;
import com.aqe.model.entity.StrategyDef;
import com.aqe.model.entity.StrategyInstance;
import com.aqe.repository.StrategyDefRepository;
import com.aqe.service.StrategyInstanceService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 策略引擎（基于 LiteFlow）
 *
 * 核心职责：
 * 1. 接收行情事件，更新行情缓存
 * 2. 查找匹配的策略实例
 * 3. 通过 LiteFlow FlowExecutor 执行策略链
 */
@Slf4j
@Service
public class StrategyEngine {

    @Autowired
    private StrategyDefRepository strategyDefRepository;

    @Autowired
    private StrategyInstanceService instanceService;

    @Autowired
    private StrategyChainLoader chainLoader;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    @Qualifier("cpuExecutor")
    private ThreadPoolTaskExecutor cpuExecutor;

    @Autowired
    private FlowExecutor flowExecutor;

    /** 节点配置缓存：key=strategyDefId, value=nodeConfigs，避免每次 tick 都查 DB + 解析 JSON */
    private final Map<Long, Map<String, Object>> nodeConfigsCache = new ConcurrentHashMap<>();

    /** 运行实例缓存：key=symbol, value=预解析后的缓存实例列表，避免每次 tick 都查 DB + 解析 JSON */
    private final Map<String, List<CachedInstance>> runningInstancesCache = new ConcurrentHashMap<>();
    private volatile boolean instancesCacheValid = false;

    /**
     * 缓存实例：预解析 triggerConditions 和 params，主链路零 JSON 解析
     */
    private static class CachedInstance {
        final Long id;
        final Long strategyDefId;
        final String symbol;
        final Integer priority;
        final Map<String, Object> parsedParams;           // 预解析的实例参数
        final ConditionMatcher.ParsedCondition condition; // 预解析的触发条件

        CachedInstance(StrategyInstance inst, ObjectMapper mapper) {
            this.id = inst.getId();
            this.strategyDefId = inst.getStrategyDefId();
            this.symbol = inst.getSymbol();
            this.priority = inst.getPriority();

            // 预解析 triggerConditions
            this.condition = ConditionMatcher.parse(inst.getTriggerConditions());

            // 预解析 params
            this.parsedParams = new HashMap<>();
            String params = inst.getParams();
            if (params != null && !params.isEmpty()) {
                try {
                    JsonNode node = mapper.readTree(params);
                    node.fields().forEachRemaining(entry -> {
                        JsonNode value = entry.getValue();
                        if (value.isNumber())      parsedParams.put(entry.getKey(), value.numberValue());
                        else if (value.isTextual()) parsedParams.put(entry.getKey(), value.asText());
                        else                        parsedParams.put(entry.getKey(), value.toString());
                    });
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    /** 性能统计：chain 执行耗时（滑动窗口） */
    private final AtomicLong totalExecCount = new AtomicLong(0);
    private final AtomicLong totalExecTimeNanos = new AtomicLong(0);
    private volatile long lastExecTimeNanos = 0;

    /**
     * 行情事件入口（由 MQ 消费者或 REST 接口调用）
     */
    public void onMarketEvent(String symbol, double price, int volume) {
        // 1. 更新行情缓存
        MarketDataCmp.updateMarket(symbol, price, volume);

        // 2. 查找该标的所有运行实例（优先从缓存获取，已预解析）
        List<CachedInstance> instances = getRunningInstances(symbol);
        if (instances.isEmpty()) return;

        // 3. 并行触发所有满足条件的实例（纯内存比较，无 JSON 解析）
        CompletableFuture<?>[] futures = instances.stream()
                .filter(inst -> inst.condition.match(price, volume))
                .map(inst -> CompletableFuture.runAsync(() -> executeInstance(inst), cpuExecutor))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).exceptionally(ex -> {
            log.error("Some instance execution failed", ex);
            return null;
        });
    }

    /**
     * 执行单个策略实例（纯内存操作，无 DB / JSON 解析）
     */
    private void executeInstance(CachedInstance instance) {
        try {
            String chainId = "chain_" + instance.strategyDefId;

            // 若 chain 不存在（可能是首次执行或缓存丢失），尝试重新加载
            if (!FlowBus.containChain(chainId)) {
                log.warn("Chain not found: {}, attempting to reload...", chainId);
                StrategyDef def = strategyDefRepository.findById(instance.strategyDefId)
                        .orElseThrow(() -> new RuntimeException("Strategy def not found: " + instance.strategyDefId));
                chainLoader.reloadChain(def);
            }

            // 构建 LiteFlow 上下文（直接使用预解析数据，无 JSON 解析）
            StrategyContext ctx = new StrategyContext();
            ctx.setInstanceId(instance.id);
            ctx.setInstancePriority(instance.priority);
            ctx.setSymbol(instance.symbol);
            ctx.getParams().putAll(instance.parsedParams);

            // 从缓存获取节点配置
            Map<String, Object> nodeConfigs = nodeConfigsCache.computeIfAbsent(
                    instance.strategyDefId,
                    defId -> {
                        String flowJson = strategyDefRepository.findById(defId)
                                .map(StrategyDef::getFlowJson)
                                .orElse("{}");
                        return chainLoader.extractNodeConfigs(flowJson);
                    }
            );
            ctx.setNodeConfigs(nodeConfigs);

            // 执行 LiteFlow Chain
            long startNanos = System.nanoTime();
            LiteflowResponse response = flowExecutor.execute2Resp(chainId, ctx);
            long elapsedNanos = System.nanoTime() - startNanos;

            // 更新性能统计
            totalExecCount.incrementAndGet();
            totalExecTimeNanos.addAndGet(elapsedNanos);
            lastExecTimeNanos = elapsedNanos;

            if (!response.isSuccess()) {
                log.error("Chain {} execution failed for instance {}", chainId, instance.id, response.getCause());
            }

            log.debug("Instance {} executed via chain {} in {} ms",
                    instance.id, chainId, elapsedNanos / 1_000_000.0);
        } catch (Exception e) {
            log.error("Execute instance {} error", instance.id, e);
        }
    }

    /**
     * 获取运行实例（缓存优先，缓存未命中或失效时查 DB 并预解析）
     */
    private List<CachedInstance> getRunningInstances(String symbol) {
        if (!instancesCacheValid) {
            Map<String, List<CachedInstance>> fresh = new ConcurrentHashMap<>();
            for (StrategyInstance inst : instanceService.findAll()) {
                if (inst.getStatus() != null && inst.getStatus() == 1) {
                    fresh.computeIfAbsent(inst.getSymbol(), k -> new java.util.ArrayList<>())
                         .add(new CachedInstance(inst, objectMapper));
                }
            }
            runningInstancesCache.clear();
            runningInstancesCache.putAll(fresh);
            instancesCacheValid = true;
            log.info("Instances cache reloaded, total symbols: {}", fresh.size());
        }
        return runningInstancesCache.getOrDefault(symbol, java.util.Collections.emptyList());
    }

    /**
     * 使实例缓存失效（实例增删改时调用）
     */
    public void invalidateInstancesCache() {
        instancesCacheValid = false;
    }

    /**
     * 获取性能统计摘要（供监控接口调用）
     */
    public Map<String, Object> getPerfStats() {
        long count = totalExecCount.get();
        long totalNanos = totalExecTimeNanos.get();
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("totalExecCount", count);
        stats.put("avgExecMs", count > 0 ? totalNanos / count / 1_000_000.0 : 0);
        stats.put("lastExecMs", lastExecTimeNanos / 1_000_000.0);
        stats.put("totalTimeMs", totalNanos / 1_000_000.0);
        return stats;
    }
}
