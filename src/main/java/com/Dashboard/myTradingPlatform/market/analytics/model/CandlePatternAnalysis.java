package com.Dashboard.myTradingPlatform.market.analytics.model;

import java.math.BigDecimal;
import java.time.Instant;

public record CandlePatternAnalysis(

        String pattern,

        PatternDirection direction,

        PatternStrength strength,

        int score,

        Instant timestamp,

        BigDecimal open,

        BigDecimal high,

        BigDecimal low,

        BigDecimal close

) {
}