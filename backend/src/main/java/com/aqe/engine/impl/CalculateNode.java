// CalculateNode.java - 使用Aviator
package com.aqe.engine.impl;

import com.aqe.engine.NodeExecutor;
import org.springframework.stereotype.Component;


import java.util.Map;
import com.googlecode.aviator.Expression;

@Component
public class CalculateNode implements NodeExecutor {

    @Override
    public Object execute(Map<String, Object> context, Map<String, Object> properties) {
        // 从 properties 中取出预编译好的 Expression 对象
        Expression compiledExpression = (Expression) properties.get("compiledExpression");
        if (compiledExpression == null) {
            throw new IllegalStateException("compiledExpression not found in properties for calculate node");
        }
        // 直接执行，传入上下文作为变量环境
        return compiledExpression.execute(context);
    }
}