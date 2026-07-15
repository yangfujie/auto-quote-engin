package com.aqe.trading;

import com.aqe.model.entity.QuoteOrder;

public interface TradingClient {
    boolean sendOrder(QuoteOrder order);
}