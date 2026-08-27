package com.Dashboard.myTradingPlatform.market.controller;

import com.Dashboard.myTradingPlatform.market.cache.MarketCandleCache;
import com.Dashboard.myTradingPlatform.market.model.MarketCandle;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/market-candles")
public class MarketCandleController {

    private final MarketCandleCache marketCandleCache;

    public MarketCandleController(MarketCandleCache marketCandleCache) {
        this.marketCandleCache = marketCandleCache;
    }

    @GetMapping("/{instrumentKey}/{timeframe}")
    public List<MarketCandle> getCandles(
            @PathVariable String instrumentKey,
            @PathVariable String timeframe) {

        return marketCandleCache.getCandles(
                instrumentKey,
                timeframe
        );
    }

    @GetMapping("/{instrumentKey}/{timeframe}/latest")
    public MarketCandle getLatestCandle(
            @PathVariable String instrumentKey,
            @PathVariable String timeframe) {

        return marketCandleCache.getLatest(
                instrumentKey,
                timeframe
        );
    }
}