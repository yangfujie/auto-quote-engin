package com.aqe.engine;

import com.aqe.engine.impl.MarketDataNode;
import com.aqe.model.entity.StrategyDef;
import com.aqe.model.entity.StrategyInstance;
import com.aqe.repository.StrategyDefRepository;
import com.aqe.service.StrategyInstanceService;
import com.aqe.util.NodeExecutionUnit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    private ObjectMapper objectMapper;

    // 注入所有 NodeExecutor 实现类，key 为 bean 名称
    @Autowired
    private Map<String, NodeExecutor> executorMap;

    private final ExecutorService executor = new ThreadPoolExecutor(
            4, 20, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    private final Map<Long, ExecutionPlan> planCache = new ConcurrentHashMap<>();

    // ========== 外部消息入口 ==========
    public void onMarketEvent(String symbol, double price, int volume) {
        MarketDataNode.updateMarket(symbol, price, volume);
        List<StrategyInstance> instances = instanceService.findRunningBySymbol(symbol);
        for (StrategyInstance inst : instances) {
            if (ConditionMatcher.match(inst.getTriggerConditions(), price, volume)) {
                executor.submit(() -> executeInstance(inst));
            }
        }
    }

    // ========== 执行单个实例 ==========
    private void executeInstance(StrategyInstance instance) {
        try {
            ExecutionPlan plan = planCache.computeIfAbsent(instance.getStrategyDefId(), this::buildPlan);
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

            plan.execute(context);
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