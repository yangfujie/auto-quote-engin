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

    /**
     * 按拓扑顺序执行所有节点，并将每个节点的执行结果以节点ID为键存入context
     */
    public void execute(Map<String, Object> context) {
        for (NodeExecutionUnit unit : units) {
            Object result = unit.getExecutor().execute(context, unit.getProperties());
            // 从properties中获取节点ID，将结果存入上下文供后续节点引用
            String nodeId = (String) unit.getProperties().get("id");
            if (nodeId != null) {
                context.put(nodeId, result);
            }
        }
    }
}