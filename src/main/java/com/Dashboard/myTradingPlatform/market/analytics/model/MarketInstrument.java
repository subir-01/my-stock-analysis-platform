package com.Dashboard.myTradingPlatform.market.analytics.model;


public record MarketInstrument(
        String instrumentKey,
        String displayName,
        String exchange,
        boolean enabled
) {
}