package com.aqe.engine;

import com.aqe.util.NodeExecutionUnit;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class ExecutionPlan {
    private List<NodeExecutionUnit> units;

    public void execute(Map<String, Object> context) {
        for (NodeExecutionUnit unit : units) {
            Object result = unit.getExecutor().execute(context, unit.getProperties());
            // 将结果存入上下文，键为节点ID
            String nodeId = (String) unit.getProperties().get("id");
            if (nodeId != null) {
                context.put(nodeId, result);
            }
        }
    }
}