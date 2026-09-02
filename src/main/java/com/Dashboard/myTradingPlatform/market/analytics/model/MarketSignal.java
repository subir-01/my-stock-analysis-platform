package com.Dashboard.myTradingPlatform.market.analytics.model;

import java.time.Instant;

public record MarketSignal(
        String instrumentKey,
        Instant timestamp,
        SignalDirection direction,
        EntryCondition entryCondition,
        int confidenceScore,
        String reason
) {
}