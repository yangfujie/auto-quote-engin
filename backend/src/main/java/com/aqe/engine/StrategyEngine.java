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

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

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

    /**
     * 行情事件入口（由 MQ 消费者或 REST 接口调用）
     */
    public void onMarketEvent(String symbol, double price, int volume) {
        // 1. 更新行情缓存
        MarketDataCmp.updateMarket(symbol, price, volume);

        // 2. 查找该标的所有运行实例
        List<StrategyInstance> instances = instanceService.findRunningBySymbol(symbol);
        if (instances.isEmpty()) return;

        // 3. 并行触发所有满足条件的实例
        CompletableFuture<?>[] futures = instances.stream()
                .filter(inst -> ConditionMatcher.match(inst.getTriggerConditions(), price, volume))
                .map(inst -> CompletableFuture.runAsync(() -> executeInstance(inst), cpuExecutor))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures).exceptionally(ex -> {
            log.error("Some instance execution failed", ex);
            return null;
        });
    }

    /**
     * 执行单个策略实例
     * 通过 LiteFlow 执行对应的 Chain
     */
    private void executeInstance(StrategyInstance instance) {
        try {
            String chainId = "chain_" + instance.getStrategyDefId();

            // 若 chain 不存在（可能是首次执行或缓存丢失），尝试重新加载
            if (!FlowBus.containChain(chainId)) {
                log.warn("Chain not found: {}, attempting to reload...", chainId);
                StrategyDef def = strategyDefRepository.findById(instance.getStrategyDefId())
                        .orElseThrow(() -> new RuntimeException("Strategy def not found: " + instance.getStrategyDefId()));
                chainLoader.reloadChain(def);
            }

            // 构建 LiteFlow 上下文
            StrategyContext ctx = new StrategyContext();
            ctx.setInstanceId(instance.getId());
            ctx.setInstancePriority(instance.getPriority());
            ctx.setSymbol(instance.getSymbol());

            // 注入实例参数（如 multiplier）
            if (instance.getParams() != null && !instance.getParams().isEmpty()) {
                JsonNode paramsNode = objectMapper.readTree(instance.getParams());
                paramsNode.fields().forEachRemaining(entry -> {
                    JsonNode value = entry.getValue();
                    if (value.isNumber())      ctx.putParam(entry.getKey(), value.numberValue());
                    else if (value.isTextual()) ctx.putParam(entry.getKey(), value.asText());
                    else                        ctx.putParam(entry.getKey(), value.toString());
                });
            }

            // 从缓存获取节点配置（首次查询后缓存，策略更新时清除缓存）
            Map<String, Object> nodeConfigs = nodeConfigsCache.computeIfAbsent(
                    instance.getStrategyDefId(),
                    defId -> {
                        String flowJson = strategyDefRepository.findById(defId)
                                .map(StrategyDef::getFlowJson)
                                .orElse("{}");
                        return chainLoader.extractNodeConfigs(flowJson);
                    }
            );
            ctx.setNodeConfigs(nodeConfigs);

            // 执行 LiteFlow Chain
            LiteflowResponse response = flowExecutor.execute2Resp(chainId, ctx);
            if (!response.isSuccess()) {
                log.error("Chain {} execution failed for instance {}", chainId, instance.getId(), response.getCause());
            }

            log.debug("Instance {} executed via chain {}", instance.getId(), chainId);
        } catch (Exception e) {
            log.error("Execute instance {} error", instance.getId(), e);
        }
    }
}
