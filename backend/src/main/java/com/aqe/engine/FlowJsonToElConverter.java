package com.aqe.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 将前端 Drawflow 导出的 flow_json 转换为 LiteFlow EL 表达式
 *
 * 转换规则：
 * - 对节点进行拓扑排序
 * - 按拓扑序生成 THEN(node1("id1"), node2("id2"), ...) 形式的 EL 表达式
 * - 每个组件的 tag 设置为节点ID，用于区分同类型不同配置的节点
 *
 * 示例输出：
 * THEN(marketDataCmp("node1"), calculateCmp("node2"), outputCmp("node3"))
 */
@Slf4j
@Component
public class FlowJsonToElConverter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 将 flow_json 转换为 LiteFlow EL 表达式字符串
     */
    public String convert(String flowJson) {
        try {
            JsonNode root = objectMapper.readTree(flowJson);
            JsonNode nodesArray = root.path("nodes");
            JsonNode connectionsArray = root.path("connections");

            // 收集节点信息
            List<String> nodeIds = new ArrayList<>();
            Map<String, String> nodeTypes = new HashMap<>();
            for (JsonNode node : nodesArray) {
                String id = node.path("id").asText();
                String type = node.path("type").asText();
                nodeIds.add(id);
                nodeTypes.put(id, toComponentId(type));
            }

            // 构建邻接表和入度
            Map<String, List<String>> adjacency = new HashMap<>();
            Map<String, Integer> inDegree = new HashMap<>();
            for (String id : nodeIds) {
                adjacency.put(id, new ArrayList<>());
                inDegree.put(id, 0);
            }
            for (JsonNode conn : connectionsArray) {
                String src = conn.path("source").asText();
                String tgt = conn.path("target").asText();
                if (adjacency.containsKey(src) && inDegree.containsKey(tgt)) {
                    adjacency.get(src).add(tgt);
                    inDegree.put(tgt, inDegree.get(tgt) + 1);
                }
            }

            // Kahn 拓扑排序
            Queue<String> queue = new LinkedList<>();
            for (Map.Entry<String, Integer> e : inDegree.entrySet()) {
                if (e.getValue() == 0) queue.offer(e.getKey());
            }
            List<String> sorted = new ArrayList<>();
            while (!queue.isEmpty()) {
                String cur = queue.poll();
                sorted.add(cur);
                for (String next : adjacency.getOrDefault(cur, Collections.emptyList())) {
                    int deg = inDegree.get(next) - 1;
                    inDegree.put(next, deg);
                    if (deg == 0) queue.offer(next);
                }
            }

            if (sorted.size() != nodeIds.size()) {
                throw new RuntimeException("Cycle detected in flow graph, cannot convert to EL");
            }

            // 生成 EL 表达式：THEN(cmp1("id1"), cmp2("id2"), ...)
            StringBuilder el = new StringBuilder();
            for (int i = 0; i < sorted.size(); i++) {
                if (i > 0) el.append(".then(");
                String nodeId = sorted.get(i);
                String componentId = nodeTypes.get(nodeId);
                el.append(componentId).append("(\"").append(nodeId).append("\")");
                if (i > 0) el.append(")");
            }

            String expression = "THEN(" + buildInnerEl(sorted, nodeTypes) + ")";
            log.info("Converted flow JSON to LiteFlow EL: {}", expression);
            return expression;

        } catch (Exception e) {
            log.error("Failed to convert flow JSON to EL", e);
            throw new RuntimeException("Failed to convert flow JSON to EL", e);
        }
    }

    /**
     * 构建内部 EL（不含外层 THEN 包装）
     */
    private String buildInnerEl(List<String> sorted, Map<String, String> nodeTypes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sorted.size(); i++) {
            if (i > 0) sb.append(", ");
            String nodeId = sorted.get(i);
            String componentId = nodeTypes.get(nodeId);
            sb.append(componentId).append(".tag(\"").append(nodeId).append("\")");
        }
        return sb.toString();
    }

    /**
     * 节点类型 → LiteFlow 组件ID
     */
    private String toComponentId(String nodeType) {
        switch (nodeType) {
            case "market":    return "marketDataCmp";
            case "calculate": return "calculateCmp";
            case "output":    return "outputCmp";
            default: throw new IllegalArgumentException("Unknown node type: " + nodeType);
        }
    }
}
