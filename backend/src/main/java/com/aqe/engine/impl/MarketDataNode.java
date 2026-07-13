package com.aqe.engine.impl;

import com.aqe.engine.NodeExecutor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MarketDataNode implements NodeExecutor {
    private static final Map<String, Map<String, Object>> marketCache = new ConcurrentHashMap<>();

    @Override
    public Object execute(Map<String, Object> context, Map<String, Object> properties) {
        String symbol = (String) properties.get("symbol");
        if (symbol == null) {
            // 如果未指定，尝试从上下文获取，或使用默认
            symbol = "AAPL";
        }
        Map<String, Object> data = marketCache.get(symbol);
        if (data == null) {
            data = new HashMap<>();
            data.put("lastPrice", 100.0);
            data.put("volume", 1000);
            data.put("timestamp", System.currentTimeMillis());
        }
        return data; // 直接返回，由上层存入上下文
    }

    public static void updateMarket(String symbol, double price, int volume) {
        Map<String, Object> data = new HashMap<>();
        data.put("lastPrice", price);
        data.put("volume", volume);
        data.put("timestamp", System.currentTimeMillis());
        marketCache.put(symbol, data);
    }
}