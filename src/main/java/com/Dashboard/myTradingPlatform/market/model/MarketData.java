package com.Dashboard.myTradingPlatform.market.model;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketData (
    String instrumentKey,
    BigDecimal lastPrice,
    Long lastTradedQuantity,
    Long volume,
    BigDecimal previousClose,
    Instant timestamp
){
}
