package com.Dashboard.myTradingPlatform.market.analytics.model;

import java.time.Instant;
import java.util.List;

public record MultiTimeframeAnalysis(

        String instrumentKey,

        Instant timestamp,

        List<TimeframeAnalysis> analyses,

        TrendAlignment alignment

) {
}