package com.Dashboard.myTradingPlatform.market.analytics.model;

public record TimeframeAnalysis(

        MarketAnalysis analysis,

        TrendDirection trendDirection,

        int bullishScore,

        int bearishScore,

        CandlePatternAnalysis candlePattern,

        MarketState marketState,

        PriceActionAnalysis priceAction,

        VolumeAnalysis volumeAnalysis,

        LiquidityAnalysis liquidity

) {
}