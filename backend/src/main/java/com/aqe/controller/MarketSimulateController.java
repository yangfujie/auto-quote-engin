package com.aqe.controller;

import com.aqe.messaging.MarketEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模拟行情端点
 */
@RestController
@RequestMapping("/api/market")
public class MarketSimulateController {

    @Autowired
    private ApplicationEventPublisher publisher;

    @PostMapping("/simulate")
    public String simulate(@RequestParam String symbol,
                           @RequestParam double price,
                           @RequestParam int volume) {
        publisher.publishEvent(new MarketEvent(symbol, price, volume));
        return "Market event published: " + symbol + " price=" + price + " volume=" + volume;
    }
}