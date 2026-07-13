package com.aqe.controller;

import com.aqe.messaging.MarketEvent;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
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
public class MarketSimulateController {

    @Autowired
    private ApplicationEventPublisher publisher;

    @ApiOperation("模拟行情推送")
    @PostMapping("/simulate")
    public String simulate(@ApiParam("股票代码") @RequestParam String symbol,
                           @ApiParam("价格") @RequestParam double price,
                           @ApiParam("数量") @RequestParam int volume) {
        publisher.publishEvent(new MarketEvent(symbol, price, volume));
        return "Market event published: " + symbol + " price=" + price + " volume=" + volume;
    }
}