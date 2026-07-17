package com.aqe.controller;

import com.aqe.engine.TickSimulator;
import com.aqe.messaging.MessagePublisher;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 行情模拟端点
 * <p>
 * 1. 单笔行情模拟（走 RabbitMQ）
 * 2. 高频 Tick 压力测试（直接调用 StrategyEngine，绕过 MQ）
 */
@Api(tags = "行情模拟")
@RestController
@RequestMapping("/api/market")
@Slf4j
public class MarketSimulateController {

    @Autowired
    private MessagePublisher messagePublisher;

    @Autowired
    private TickSimulator tickSimulator;

    // ==================== 单笔行情模拟 ====================

    @ApiOperation("模拟单笔行情（走 RabbitMQ）")
    @PostMapping("/simulate")
    public String simulate(@RequestParam String symbol,
                           @RequestParam double price,
                           @RequestParam(required = false, defaultValue = "1000") int volume) {
        log.info("Simulating market event: symbol={}, price={}, volume={}", symbol, price, volume);
        messagePublisher.publishMarket(symbol, price, volume);
        return String.format("Market event published via RabbitMQ: %s price=%.2f volume=%d",
                symbol, price, volume);
    }

    // ==================== 高频 Tick 压力测试 ====================

    /**
     * 启动高频 Tick 模拟
     * <p>
     * 直接调用 StrategyEngine.onMarketEvent()，绕过 MQ，
     * 用于对策略引擎进行纯计算压力测试。
     * <p>
     * 用法示例：
     * POST /api/market/tick/start?symbol=AAPL&basePrice=150&tps=1000&duration=10
     *
     * @param symbol     标的代码，默认 AAPL
     * @param basePrice  基准价格（随机游走起点），默认 150.0
     * @param baseVolume 基准成交量，默认 1000
     * @param tps        每秒 Tick 数量（压力大小），默认 1000
     * @param duration   持续秒数，0 表示手动停止，默认 10
     */
    @ApiOperation("启动高频Tick压力测试")
    @PostMapping("/tick/start")
    public String startTick(
            @RequestParam(required = false, defaultValue = "AAPL") String symbol,
            @RequestParam(required = false, defaultValue = "150.0") double basePrice,
            @RequestParam(required = false, defaultValue = "1000") int baseVolume,
            @RequestParam(required = false, defaultValue = "1000") int tps,
            @RequestParam(required = false, defaultValue = "10") long duration) {

        long durationMs = duration * 1000;
        log.info("Starting tick stress test: symbol={}, basePrice={}, tps={}, duration={}s",
                symbol, basePrice, tps, duration);

        try {
            tickSimulator.start(symbol, basePrice, baseVolume, tps, durationMs);
            return String.format("Tick simulator started: symbol=%s, basePrice=%.2f, tps=%d, duration=%ds",
                    symbol, basePrice, tps, duration);
        } catch (IllegalStateException e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 停止高频 Tick 模拟
     */
    @ApiOperation("停止高频Tick压力测试")
    @PostMapping("/tick/stop")
    public String stopTick() {
        tickSimulator.stop();
        return "Tick simulator stopped. " + tickSimulator.getStats();
    }

    /**
     * 查询 Tick 模拟器状态和统计
     */
    @ApiOperation("查询Tick模拟器状态")
    @GetMapping("/tick/status")
    public String tickStatus() {
        return tickSimulator.getStats();
    }
}
