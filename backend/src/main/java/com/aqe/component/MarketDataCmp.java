package com.aqe.component;

import com.aqe.context.StrategyContext;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.extern.slf4j.Slf4j;
import com.yomahub.liteflow.annotation.LiteflowComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LiteFlow 行情数据组件
 * tag = 节点ID，用于从 nodeConfigs 中获取 symbol 配置
 */
@Slf4j
@LiteflowComponent("marketDataCmp")
public class MarketDataCmp extends NodeComponent {

    private static final Map<String, Map<String, Object>> marketCache = new ConcurrentHashMap<>();

    @Override
    public void process() throws Exception {
        StrategyContext ctx = (StrategyContext) this.getRequestData();
        if (ctx == null) {
            throw new IllegalStateException("StrategyContext is null, requestData not set");
        }
        String nodeId = this.getTag();

        // 从节点配置中获取 symbol，若无则使用上下文中的 symbol
        String symbol = null;
        if (nodeId != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> config = (Map<String, Object>) ctx.getNodeConfigs().get(nodeId);
            if (config != null) {
                symbol = (String) config.get("symbol");
            }
        }
        if (symbol == null || symbol.isEmpty()) {
            symbol = ctx.getSymbol();
        }

        Map<String, Object> data = marketCache.get(symbol);
        if (data != null) {
            ctx.setLastPrice((Double) data.get("lastPrice"));
            ctx.setMarketVolume((Integer) data.get("volume"));
        } else {
            log.warn("No market data cached for symbol: {}", symbol);
        }
        log.debug("MarketDataCmp [{}]: symbol={}, price={}, volume={}",
                nodeId, symbol, ctx.getLastPrice(), ctx.getMarketVolume());
    }

    /**
     * 更新行情缓存（由 StrategyEngine 在行情事件到来时调用）
     */
    public static void updateMarket(String symbol, double price, int volume) {
        Map<String, Object> data = new HashMap<>();
        data.put("lastPrice", price);
        data.put("volume", volume);
        data.put("timestamp", System.currentTimeMillis());
        marketCache.put(symbol, data);
    }
}
