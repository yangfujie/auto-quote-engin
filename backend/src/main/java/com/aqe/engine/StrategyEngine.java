package com.aqe.engine;

import com.aqe.engine.impl.MarketDataNode;
import com.aqe.model.entity.QuoteOrder;
import com.aqe.model.entity.StrategyDef;
import com.aqe.model.entity.StrategyInstance;
import com.aqe.repository.StrategyDefRepository;
import com.aqe.service.QuoteOrderService;
import com.aqe.service.StrategyInstanceService;
import com.aqe.util.NodeExecutionUnit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
public class StrategyEngine {

    @Autowired
    private StrategyDefRepository strategyDefRepository;
    @Autowired
    private StrategyInstanceService instanceService;


    @Autowired
    private QuoteOrderService orderService;
    private ObjectMapper objectMapper;

    // 注入所有 NodeExecutor 实现类，key 为 bean 名称
    @Autowired
    private Map<String, NodeExecutor> executorMap;


    @Autowired
    @Qualifier("cpuExecutor")
    private ThreadPoolTaskExecutor cpuExecutor;

    @Autowired
    @Qualifier("ioExecutor")
    private ThreadPoolTaskExecutor ioExecutor;

    private final Map<Long, ExecutionPlan> planCache = new ConcurrentHashMap<>();

    /**
     * 行情事件入口（由MQ消费者调用）
     */
    public void onMarketEvent(String symbol, double price, int volume) {
        // 更新行情缓存
        MarketDataNode.updateMarket(symbol, price, volume);
        // 查找该标的所有运行实例
        List<StrategyInstance> instances = instanceService.findRunningBySymbol(symbol);
        if (instances.isEmpty()) return;

        // 使用 CompletableFuture 批量提交，实现并行触发
        CompletableFuture<?>[] futures = instances.stream()
                .filter(inst -> ConditionMatcher.match(inst.getTriggerConditions(), price, volume))
                .map(inst -> CompletableFuture.runAsync(() -> executeInstance(inst), cpuExecutor))
                .toArray(CompletableFuture[]::new);

        // 可选：等待所有实例执行完成（或设置超时）
        CompletableFuture.allOf(futures).exceptionally(ex -> {
            log.error("Some instance execution failed", ex);
            return null;
        });
    }


    // ========== 执行单个实例 ==========
    private void executeInstance(StrategyInstance instance) {
        try {
            // 1. 获取执行计划（缓存）
            ExecutionPlan plan = planCache.computeIfAbsent(instance.getStrategyDefId(), this::buildPlan);

            // 2. 构建上下文
            Map<String, Object> context = new HashMap<>();
            context.put("instanceId", instance.getId());
            context.put("instancePriority", instance.getPriority());

            // 将实例的参数覆盖放入 context（如 multiplier）
            if (instance.getParams() != null && !instance.getParams().isEmpty()) {
                JsonNode paramsNode = objectMapper.readTree(instance.getParams());
                paramsNode.fields().forEachRemaining(entry -> {
                    JsonNode value = entry.getValue();
                    if (value.isNumber()) context.put(entry.getKey(), value.numberValue());
                    else if (value.isTextual()) context.put(entry.getKey(), value.asText());
                    else context.put(entry.getKey(), value.toString());
                });
            }

            // 3. 执行DAG（纯计算，无阻塞IO）
            plan.execute(context);

            // 4. 异步持久化订单（IO密集型）—— 使用 ioExecutor
            Object orderObj = context.get("outputOrder");
            if (orderObj instanceof QuoteOrder) {
                QuoteOrder order = (QuoteOrder) orderObj;
                CompletableFuture.runAsync(() -> {
                    // 保存到数据库或发送MQ
                    orderService.save(order);
                    log.debug("Order persisted: {}", order.getId());
                }, ioExecutor);
            }
            log.info("Instance {} executed, order generated.", instance.getId());
        } catch (Exception e) {
            log.error("Execute instance {} error", instance.getId(), e);
        }
    }

    // ========== 构建执行计划（核心） ==========
    private ExecutionPlan buildPlan(Long defId) {
        StrategyDef def = strategyDefRepository.findById(defId)
                .orElseThrow(() -> new RuntimeException("Strategy def not found: " + defId));
        String flowJson = def.getFlowJson();

        try {
            JsonNode root = objectMapper.readTree(flowJson);
            JsonNode nodesArray = root.path("nodes");
            JsonNode connectionsArray = root.path("connections");

            Map<String, JsonNode> nodeMap = new HashMap<>();
            for (JsonNode node : nodesArray) {
                String id = node.path("id").asText();
                nodeMap.put(id, node);
            }

            // 构建邻接表
            Map<String, List<String>> adjacencyList = new HashMap<>();
            for (JsonNode conn : connectionsArray) {
                String source = conn.path("source").asText();
                String target = conn.path("target").asText();
                adjacencyList.computeIfAbsent(source, k -> new ArrayList<>()).add(target);
            }

            // 计算入度
            Map<String, Integer> inDegree = new HashMap<>();
            for (String nodeId : nodeMap.keySet()) {
                inDegree.put(nodeId, 0);
            }
            for (Map.Entry<String, List<String>> entry : adjacencyList.entrySet()) {
                for (String target : entry.getValue()) {
                    inDegree.put(target, inDegree.getOrDefault(target, 0) + 1);
                }
            }

            // 拓扑排序
            Queue<String> queue = new LinkedList<>();
            for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
                if (entry.getValue() == 0) {
                    queue.offer(entry.getKey());
                }
            }
            List<String> sortedIds = new ArrayList<>();
            while (!queue.isEmpty()) {
                String nodeId = queue.poll();
                sortedIds.add(nodeId);
                List<String> neighbors = adjacencyList.getOrDefault(nodeId, Collections.emptyList());
                for (String neighbor : neighbors) {
                    int deg = inDegree.get(neighbor) - 1;
                    inDegree.put(neighbor, deg);
                    if (deg == 0) {
                        queue.offer(neighbor);
                    }
                }
            }
            if (sortedIds.size() != nodeMap.size()) {
                throw new RuntimeException("Cycle detected in flow graph for strategy def: " + defId);
            }

            // 构建执行单元
            List<NodeExecutionUnit> units = new ArrayList<>();
            for (String nodeId : sortedIds) {
                JsonNode node = nodeMap.get(nodeId);
                String type = node.path("type").asText();

                Map<String, Object> properties = new HashMap<>();
                properties.put("id", nodeId);

                switch (type) {
                    case "market":
                        if (node.has("symbol")) {
                            properties.put("symbol", node.path("symbol").asText());
                        }
                        break;
                    case "calculate":
                        String expressionStr = node.path("expression").asText();
                        if (expressionStr == null || expressionStr.isEmpty()) {
                            throw new RuntimeException("Calculate node missing expression: " + nodeId);
                        }
                        // 预编译表达式
                        Expression compiled = AviatorEvaluator.compile(expressionStr, true);
                        properties.put("compiledExpression", compiled);
                        // 可选：保留原始表达式用于日志
                        properties.put("expression", expressionStr);
                        break;
                    case "output":
                        if (node.has("priceRef")) properties.put("priceRef", node.path("priceRef").asText());
                        if (node.has("volumeRef")) properties.put("volumeRef", node.path("volumeRef").asText());
                        if (node.has("sideRef")) properties.put("sideRef", node.path("sideRef").asText());
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown node type: " + type);
                }

                NodeExecutor executor = getExecutorByType(type);
                units.add(new NodeExecutionUnit(executor, properties));
            }

            log.info("Built execution plan for def {} with {} nodes", defId, units.size());
            return new ExecutionPlan(units);

        } catch (Exception e) {
            log.error("Failed to build execution plan for def {}", defId, e);
            throw new RuntimeException("Failed to build execution plan", e);
        }
    }

    // ========== 根据节点类型获取执行器 ==========
    private NodeExecutor getExecutorByType(String type) {
        switch (type) {
            case "market":    return executorMap.get("marketDataNode");
            case "calculate": return executorMap.get("calculateNode");
            case "output":    return executorMap.get("outputNode");
            default: throw new IllegalArgumentException("Unknown node type: " + type);
        }
    }
}