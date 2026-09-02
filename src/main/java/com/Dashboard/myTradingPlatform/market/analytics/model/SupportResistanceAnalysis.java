package com.Dashboard.myTradingPlatform.market.analytics.model;

import java.math.BigDecimal;

public record SupportResistanceAnalysis(

        String timeframe,

        BigDecimal price,

        BigDecimal support,

        BigDecimal resistance,

        PriceLevelPosition position,

        BigDecimal distanceFromSupportPercent,

        BigDecimal distanceFromResistancePercent

) {
}