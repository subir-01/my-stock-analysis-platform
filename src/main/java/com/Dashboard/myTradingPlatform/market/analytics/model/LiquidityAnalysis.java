package com.Dashboard.myTradingPlatform.market.analytics.model;

import java.math.BigDecimal;
import java.util.List;

public record LiquidityAnalysis(

        BigDecimal currentPrice,

        List<LiquidityZone> supports,

        List<LiquidityZone> resistances

) {
}