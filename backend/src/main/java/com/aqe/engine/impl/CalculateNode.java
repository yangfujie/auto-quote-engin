// CalculateNode.java - 使用Aviator
package com.aqe.engine.impl;

import com.aqe.engine.NodeExecutor;
import com.googlecode.aviator.AviatorEvaluator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CalculateNode implements NodeExecutor {
    @Override
    public Object execute(Map<String, Object> context, Map<String, Object> properties) {
        String expression = (String) properties.get("expression");
        // 构建变量环境，从context中取值
        Map<String, Object> env = new HashMap<>();
        context.forEach((k, v) -> {
            if (v instanceof Number || v instanceof String || v instanceof Boolean) {
                env.put(k, v);
            } else if (v instanceof Map) {
                // 处理嵌套，如 market.lastPrice -> 需要展平
                ((Map<String, Object>) v).forEach((subKey, subVal) -> env.put(k + "." + subKey, subVal));
            }
        });
        Object result = AviatorEvaluator.execute(expression, env);
        // 将计算结果存入context，节点ID作为key
        String nodeId = (String) properties.get("nodeId");
        context.put(nodeId, result);
        return result;
    }
}
