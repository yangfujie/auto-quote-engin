// MonitorController.java - 提供队列深度、订单查询等
package com.aqe.controller;

import com.aqe.messaging.MarketEvent;
import com.aqe.model.entity.QuoteOrder;
import com.aqe.queue.PriorityOrderQueue;
import com.aqe.service.QuoteOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/monitor")
public class MonitorController {
    @Autowired private PriorityOrderQueue orderQueue;
    @Autowired private QuoteOrderService orderService;

    @GetMapping("/queueDepth")
    public int queueDepth() {
        return orderQueue.size();
    }

    @GetMapping("/orders")
    public List<QuoteOrder> recentOrders() {
        return orderService.findRecent(100);
    }


//    @PostMapping("/simulateMarket")
//    public void simulateMarket(@RequestParam String symbol, @RequestParam double price, @RequestParam int volume) {
//        applicationEventPublisher.publishEvent(new MarketEvent(symbol, price, volume));
//    }
}