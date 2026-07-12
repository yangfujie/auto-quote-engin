// ExecutionPlan.java
package com.aqe.engine;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class ExecutionPlan {
    private List<NodeExecutor> sortedNodes;
    private List<String> nodeIds; // 对应顺序

    public ExecutionPlan(List<NodeExecutor> nodes) {
    }

    public void execute(Map<String, Object> context, Map<String, Object> params) {
        for (NodeExecutor node : sortedNodes) {
            node.execute(context, params);
        }
    }
}

// 构建计划的服务（简化，实际需要解析flow_json构建DAG）
// 这里我们直接在引擎中解析flow_json，暂不提供完整拓扑排序，仅演示