package com.Dashboard.myTradingPlatform.market.analytics.model;

import java.time.Instant;

public record MarketScore(

        String instrumentKey,

        Instant timestamp,

        int bullishScore,

        int bearishScore,

        int totalScore,

        double bullishPercentage,

        double bearishPercentage

) {
}