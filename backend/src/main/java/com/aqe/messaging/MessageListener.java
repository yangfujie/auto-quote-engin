// MessageListener.java
package com.aqe.messaging;

import com.aqe.engine.StrategyEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MessageListener {
    @Autowired
    private StrategyEngine strategyEngine;

    @EventListener
    public void handleMarketEvent(MarketEvent event) {
        log.info("Received market event: {}", event);
        strategyEngine.onMarketEvent(event.getSymbol(), event.getPrice(), event.getVolume());
    }
}