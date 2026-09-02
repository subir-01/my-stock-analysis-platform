package com.Dashboard.myTradingPlatform.market.analytics.model;

import java.math.BigDecimal;
import java.time.Instant;

public record LiquidityZone(

        BigDecimal level,

        int distancePoints,

        int liquidityScore,

        int touches,

        int rejectionCount,

        int swingPointCount,

        LiquidityZoneType type,

        Instant firstSeen,

        Instant lastSeen

) {
}