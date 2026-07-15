package com.aqe.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketData implements Serializable {
    private String symbol;
    private double price;
    private int volume;
    private long timestamp;
}