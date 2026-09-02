package com.Dashboard.myTradingPlatform.market.analytics.model;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceActionAnalysis(

        String timeframe,

        Instant timestamp,

        PriceActionSignal signal,

        BigDecimal price,

        BigDecimal previousClose,

        BigDecimal support,

        BigDecimal resistance,

        boolean confirmed

) {
}