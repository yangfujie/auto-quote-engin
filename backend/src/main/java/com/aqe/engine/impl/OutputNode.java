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

    @Override
    public Object execute(Map<String, Object> context, Map<String, Object> properties) {
        String priceRef = (String) properties.get("priceRef");
        String volumeRef = (String) properties.get("volumeRef");
        String sideRef = (String) properties.get("sideRef");
        // 从context获取值
        Double price = (Double) context.get(priceRef);
        Integer volume = (Integer) context.get(volumeRef);
        String side = (String) context.get(sideRef);
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
        // 入队
        orderQueue.offer(order);
        context.put("outputOrder", order);
        return order;
    }
}