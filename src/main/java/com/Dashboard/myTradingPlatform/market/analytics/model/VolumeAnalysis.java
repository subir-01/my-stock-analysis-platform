package com.Dashboard.myTradingPlatform.market.analytics.model;

import java.math.BigDecimal;

public record VolumeAnalysis(

        String timeframe,

        BigDecimal currentVolume,

        BigDecimal averageVolume,

        BigDecimal volumeRatio,

        VolumeCondition condition,

        boolean volumeConfirmed

) {
}