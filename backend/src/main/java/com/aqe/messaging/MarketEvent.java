package com.aqe.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MarketEvent {
    private String symbol;
    private double price;
    private int volume;
}