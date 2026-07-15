package com.aqe.controller;


import com.aqe.messaging.MessagePublisher;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模拟行情端点
 */
@Api(tags = "行情模拟")
@RestController
@RequestMapping("/api/market")
@Slf4j
public class MarketSimulateController {

    @Autowired
    private MessagePublisher messagePublisher;

    @ApiOperation("模拟行情推送")
    /**
     * 模拟行情数据发送（通过 RabbitMQ）
     * 用法：POST /api/market/simulate?symbol=AAPL&price=150.5&volume=2000
     */
    @PostMapping("/simulate")
    public String simulate(@RequestParam String symbol,
                           @RequestParam double price,
                           @RequestParam(required = false, defaultValue = "1000") int volume) {
        log.info("Simulating market event: symbol={}, price={}, volume={}", symbol, price, volume);

        // 发送到 RabbitMQ
        messagePublisher.publishMarket(symbol, price, volume);

        return String.format("Market event published via RabbitMQ: %s price=%.2f volume=%d",
                symbol, price, volume);
    }
}