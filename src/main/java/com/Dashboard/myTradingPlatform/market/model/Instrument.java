package com.Dashboard.myTradingPlatform.market.model;


public record Instrument(
        String instrumentKey,
        String symbol,
        String exchange,
        String type
) {
}