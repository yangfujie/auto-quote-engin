package com.aqe.component;

import com.aqe.context.StrategyContext;
import com.aqe.model.entity.QuoteOrder;
import com.aqe.queue.PriorityOrderQueue;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

/**
 * LiteFlow 输出组件
 * tag = 节点ID，用于从 nodeConfigs 中获取 priceRef/volumeRef/sideRef
 */
@Slf4j
@LiteflowComponent("outputCmp")
public class OutputCmp extends NodeComponent {

    @Autowired
    private PriorityOrderQueue orderQueue;

    @Override
    public void process() throws Exception {
        StrategyContext ctx = (StrategyContext) this.getRequestData();
        if (ctx == null) {
            throw new IllegalStateException("StrategyContext is null, requestData not set");
        }
        String nodeId = this.getTag();
        if (nodeId == null || nodeId.isEmpty()) {
            throw new IllegalStateException("Node tag is null or empty, please set tag for output node");
        }

        Map<String, Object> nodeConfigs = ctx.getNodeConfigs();
        if (nodeConfigs == null) {
            throw new IllegalStateException("nodeConfigs is null in context");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> config = (Map<String, Object>) nodeConfigs.get(nodeId);
        if (config == null) {
            throw new IllegalStateException("Node config not found for output node: " + nodeId);
        }

        String priceRef  = (String) config.get("priceRef");
        String volumeRef = (String) config.get("volumeRef");
        String sideRef   = (String) config.get("sideRef");

        double price = resolveValue(priceRef, ctx);
        int    volume = (int) resolveValue(volumeRef, ctx);
        String side   = sideRef != null ? sideRef : "BUY";

        QuoteOrder order = new QuoteOrder();
        order.setInstanceId(ctx.getInstanceId());
        order.setPrice(BigDecimal.valueOf(price));
        order.setVolume(volume);
        order.setSide("BUY".equalsIgnoreCase(side) ? 1 : 2);
        order.setPriority(ctx.getInstancePriority() != null ? ctx.getInstancePriority() : 5);
        order.setStatus(0);
        order.setCreateTime(new Date());

        // 放入优先级队列，等待 OrderDispatcher 分发
        orderQueue.offer(order);
        ctx.setOutputOrder(order);

        log.debug("OutputCmp [{}]: order generated, price={}, volume={}, side={}",
                nodeId, price, volume, side);
    }

    /**
     * 解析引用值：支持固定数值或引用节点ID（从 params 中获取）
     */
    private double resolveValue(String ref, StrategyContext ctx) {
        if (ref == null) return 0.0;
        // 纯数字 → 固定值
        if (ref.matches("\\d+(\\.\\d+)?")) {
            return Double.parseDouble(ref);
        }
        // 否则视为节点ID，从 params 中获取计算结果
        Object val = ctx.getParam(ref);
        return val instanceof Number ? ((Number) val).doubleValue() : 0.0;
    }
}
