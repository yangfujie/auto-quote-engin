// StrategyEngine.java
package com.aqe.engine;

import com.aqe.engine.impl.MarketDataNode;
import com.aqe.model.entity.StrategyInstance;
import com.aqe.service.StrategyInstanceService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
public class StrategyEngine {
    @Autowired
    private StrategyInstanceService instanceService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private List<NodeExecutor> nodeExecutors; // Spring自动注入所有实现

    private final ExecutorService executor = new ThreadPoolExecutor(
            4, 20, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            new ThreadPoolExecutor.CallerRunsPolicy());

    private final Map<Long, ExecutionPlan> planCache = new ConcurrentHashMap<>();

    // 外部消息入口（行情事件）
    public void onMarketEvent(String symbol, double price, int volume) {
        // 更新行情缓存
        MarketDataNode.updateMarket(symbol, price, volume);
        // 查找该标的所有运行实例
        List<StrategyInstance> instances = instanceService.findRunningBySymbol(symbol);
        for (StrategyInstance inst : instances) {
            // 检查触发条件
            if (ConditionMatcher.match(inst.getTriggerConditions(), price, volume)) {
                executor.submit(() -> executeInstance(inst));
            }
        }
    }

    private void executeInstance(StrategyInstance instance) {
        try {
            // 获取或构建执行计划
            ExecutionPlan plan = planCache.computeIfAbsent(instance.getStrategyDefId(), id -> buildPlan(id));
            Map<String, Object> context = new HashMap<>();
            context.put("instanceId", instance.getId());
            context.put("instancePriority", instance.getPriority());
            // 解析params JSON并放入context
            JsonNode paramsNode = objectMapper.readTree(instance.getParams());
            if (paramsNode.isObject()) {
                paramsNode.fields().forEachRemaining(entry -> context.put(entry.getKey(), entry.getValue().asText()));
            }
            // 执行计划
            plan.execute(context, instance.getParams() == null ? new HashMap<>() : objectMapper.readValue(instance.getParams(), Map.class));
            log.info("Instance {} executed, order generated.", instance.getId());
        } catch (Exception e) {
            log.error("Execute instance {} error", instance.getId(), e);
        }
    }

    private ExecutionPlan buildPlan(Long defId) {
        // 从数据库加载flow_json，解析节点并排序（简单实现仅按节点顺序）
        // 实际需做拓扑排序，此处简化，假设节点列表已排好序
        List<NodeExecutor> nodes = new ArrayList<>();
        // TODO: 从StrategyDef获取flowJson，解析节点类型并匹配对应的executor
        // 这里模拟两个节点：行情->计算->输出
        // 实际需根据节点type选择实现类，通过Spring容器获取
        return new ExecutionPlan(nodes);
    }
}