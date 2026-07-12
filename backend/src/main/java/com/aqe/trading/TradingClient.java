//TradingClient.java

package com.aqe.trading;

import com.aqe.model.entity.QuoteOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TradingClient {
    public boolean sendOrder(QuoteOrder order) {
        // 模拟发送，随机成功
        boolean success = Math.random() > 0.1;
        log.info("Sending order: side={}, price={}, volume={}, result={}",
                order.getSide()==1?"BUY":"SELL", order.getPrice(), order.getVolume(), success);
        return success;
    }
}
