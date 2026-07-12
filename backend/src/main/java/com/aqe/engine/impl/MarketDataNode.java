package com.aqe.engine.impl;

import com.aqe.engine.NodeExecutor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MarketDataNode implements NodeExecutor {
    // 模拟行情缓存，实际可从Redis读取
    private static final Map<String, Map<String, Object>> marketCache = new ConcurrentHashMap<>();

    @Override
    public Object execute(Map<String, Object> context, Map<String, Object> properties) {
        String symbol = (String) properties.get("symbol");
        // 若上下文中已有行情则直接使用
        if (context.containsKey("marketData")) {
            return context.get("marketData");
        }
        // 模拟获取行情
        Map<String, Object> data = marketCache.get(symbol);
        if (data == null) {
            // 默认值
            data = new HashMap<>();
            data.put("lastPrice", 100.0);
            data.put("volume", 1000);
            data.put("timestamp", System.currentTimeMillis());
        }
        context.put("marketData", data);
        return data;
    }

    // 供外部更新行情
    public static void updateMarket(String symbol, double price, int volume) {
        Map<String, Object> data = new HashMap<>();
        data.put("lastPrice", price);
        data.put("volume", volume);
        data.put("timestamp", System.currentTimeMillis());
        marketCache.put(symbol, data);
    }
}
