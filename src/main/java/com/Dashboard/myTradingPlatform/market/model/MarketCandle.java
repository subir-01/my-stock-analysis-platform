package com.Dashboard.myTradingPlatform.market.model;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketCandle(
        String instrumentKey,
        String timeframe,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        Long volume,
        Instant timestamp
) {
}