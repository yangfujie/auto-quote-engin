// MonitorController.java - 提供队列深度、订单查询等
package com.aqe.controller;

import com.aqe.messaging.MarketEvent;
import com.aqe.model.entity.QuoteOrder;
import com.aqe.queue.PriorityOrderQueue;
import com.aqe.service.QuoteOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "监控")
@RestController
@RequestMapping("/api/monitor")
public class MonitorController {
    @Autowired private PriorityOrderQueue orderQueue;
    @Autowired private QuoteOrderService orderService;

    @ApiOperation("查询队列深度")
    @GetMapping("/queueDepth")
    public int queueDepth() {
        return orderQueue.size();
    }

    @ApiOperation("查询最近订单")
    @GetMapping("/orders")
    public List<QuoteOrder> recentOrders() {
        return orderService.findRecent(100);
    }

}