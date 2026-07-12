// OrderDispatcher.java
package com.aqe.queue;

import com.aqe.model.entity.QuoteOrder;
import com.aqe.service.QuoteOrderService;
import com.aqe.trading.TradingClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class OrderDispatcher {
    @Autowired
    private PriorityOrderQueue orderQueue;
    @Autowired
    private QuoteOrderService orderService;
    @Autowired
    private TradingClient tradingClient;

    @Scheduled(fixedDelay = 100) // 每100ms拉取一次
    public void dispatch() {
        List<QuoteOrder> batch = new ArrayList<>();
        orderQueue.drainTo(batch, 50);
        if (batch.isEmpty()) return;
        log.info("Dispatching {} orders", batch.size());
        for (QuoteOrder order : batch) {
            // 先持久化（实际可异步）
            orderService.save(order);
            // 调用交易接口
            boolean success = tradingClient.sendOrder(order);
            order.setStatus(success ? 1 : 3); // 1成功 3失败
            orderService.update(order);
            log.info("Order {} sent, success={}", order.getId(), success);
        }
    }
}
