package com.Dashboard.myTradingPlatform.market.analytics.model;

public record MarketState(
        MarketRegime regime,
        MomentumState momentum,
        MomentumCondition momentumCondition
) {
}