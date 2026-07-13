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
        // 直接使用context作为环境，Aviator可以处理嵌套Map
        Object result = AviatorEvaluator.execute(expression, context);
        return result;
    }
}