// OutputNode.java - 生成订单
package com.aqe.engine.impl;

import com.aqe.engine.NodeExecutor;
import com.aqe.model.entity.QuoteOrder;
import com.aqe.queue.PriorityOrderQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

@Component
public class OutputNode implements NodeExecutor {
    @Autowired
    private PriorityOrderQueue orderQueue;

    private Object resolveValue(String ref, Map<String, Object> context) {
        if (ref == null) return null;
        // 如果引用是纯数字，直接解析 支持引用节点ID或固定数值
        if (ref.matches("\\d+(\\.\\d+)?")) {
            return Double.parseDouble(ref);
        }
        // 否则从上下文获取（节点ID）
        return context.get(ref);
    }

    @Override
    public Object execute(Map<String, Object> context, Map<String, Object> properties) {
        String priceRef = (String) properties.get("priceRef");
        String volumeRef = (String) properties.get("volumeRef");
        String sideRef = (String) properties.get("sideRef");

        Object priceObj = resolveValue(priceRef, context);
        Object volumeObj = resolveValue(volumeRef, context);

        double price = priceObj instanceof Number ? ((Number) priceObj).doubleValue() : 0.0;
        int volume = volumeObj instanceof Number ? ((Number) volumeObj).intValue() : 0;
        String side = sideRef != null ? sideRef : "BUY";

        Integer priority = (Integer) context.get("instancePriority");
        Long instanceId = (Long) context.get("instanceId");

        QuoteOrder order = new QuoteOrder();
        order.setInstanceId(instanceId);
        order.setPrice(BigDecimal.valueOf(price));
        order.setVolume(volume);
        order.setSide("BUY".equalsIgnoreCase(side) ? 1 : 2);
        order.setPriority(priority != null ? priority : 5);
        order.setStatus(0);
        order.setCreateTime(new Date());

        orderQueue.offer(order);
        context.put("outputOrder", order);
        return order;
    }
}