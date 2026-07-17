package com.aqe.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 高频 Tick 行情模拟器
 * <p>
 * 直接调用 StrategyEngine.onMarketEvent()，绕过 MQ，
 * 用于对策略引擎进行纯计算压力测试。
 * <p>
 * 价格模型：随机游走（Random Walk），基于基准价格小幅波动
 * 成交量模型：随机整数 [baseVolume*0.5, baseVolume*1.5]
 */
@Slf4j
@Service
public class TickSimulator {

    @Autowired
    private StrategyEngine strategyEngine;

    private ScheduledExecutorService scheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong totalTicks = new AtomicLong(0);
    private final AtomicLong totalTriggered = new AtomicLong(0);
    private long startTimeMs;

    /**
     * 启动 Tick 模拟
     *
     * @param symbol     标的代码（如 AAPL）
     * @param basePrice  基准价格（随机游走起点）
     * @param baseVolume 基准成交量（实际在 [0.5x, 1.5x] 之间随机）
     * @param tps        每秒 Tick 数量（压力大小）
     * @param durationMs 持续时长（毫秒），0 表示直到手动停止
     */
    public synchronized void start(String symbol, double basePrice, int baseVolume, int tps, long durationMs) {
        if (running.get()) {
            throw new IllegalStateException("Tick simulator is already running. Call stop() first.");
        }

        running.set(true);
        totalTicks.set(0);
        totalTriggered.set(0);
        startTimeMs = System.currentTimeMillis();

        // 计算每 tick 间隔（纳秒精度）
        long intervalNanos = 1_000_000_000L / tps;
        log.info("Starting tick simulator: symbol={}, basePrice={}, tps={}, duration={}ms",
                symbol, basePrice, tps, durationMs);

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "tick-simulator");
            t.setDaemon(true);
            return t;
        });

        // 当前价格（随机游走）
        final double[] currentPrice = {basePrice};

        // 提交定时任务：以固定频率生成 tick
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // 随机游走：±0.1% 以内波动
                double change = (Math.random() - 0.5) * basePrice * 0.002;
                currentPrice[0] = Math.max(currentPrice[0] + change, basePrice * 0.8);
                currentPrice[0] = Math.min(currentPrice[0], basePrice * 1.2);

                // 随机成交量
                int volume = baseVolume / 2 + (int) (Math.random() * baseVolume);

                totalTicks.incrementAndGet();
                strategyEngine.onMarketEvent(symbol, currentPrice[0], volume);

            } catch (Exception e) {
                log.error("Tick generation error", e);
            }
        }, 0, intervalNanos, TimeUnit.NANOSECONDS);

        // 如果指定了持续时间，自动停止
        if (durationMs > 0) {
            scheduler.schedule(() -> {
                stop();
                log.info("Tick simulator auto-stopped after {}ms", durationMs);
            }, durationMs, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 停止 Tick 模拟
     */
    public synchronized void stop() {
        if (!running.get()) return;
        running.set(false);
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        long elapsed = System.currentTimeMillis() - startTimeMs;
        log.info("Tick simulator stopped. Total ticks={}, elapsed={}ms, avg tps={:.1f}",
                totalTicks.get(), elapsed,
                elapsed > 0 ? (totalTicks.get() * 1000.0 / elapsed) : 0);
    }

    /**
     * 是否正在运行
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * 获取统计信息
     */
    public String getStats() {
        long elapsed = System.currentTimeMillis() - startTimeMs;
        long ticks = totalTicks.get();
        double avgTps = elapsed > 0 ? (ticks * 1000.0 / elapsed) : 0;
        return String.format(
                "running=%s, totalTicks=%d, elapsed=%dms, avgTps=%.1f",
                running.get(), ticks, elapsed, avgTps
        );
    }

    public long getTotalTicks() {
        return totalTicks.get();
    }
}
