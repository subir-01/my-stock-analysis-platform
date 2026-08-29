package com.Dashboard.myTradingPlatform.market.analytics.model;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketAnalysis(

        String instrumentKey,

        String timeframe,

        Instant timestamp,

        BigDecimal price,

        BigDecimal sma20,

        BigDecimal ema20,

        BigDecimal ema50,

        BigDecimal rsi14,

        BigDecimal vwap,

        BigDecimal support,

        BigDecimal resistance
) {
}