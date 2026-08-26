package com.Dashboard.myTradingPlatform.market.controller;

import com.Dashboard.myTradingPlatform.market.cache.MarketDataCache;
import com.Dashboard.myTradingPlatform.market.model.MarketData;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/market-data")
public class MarketDataController {

    private final MarketDataCache marketDataCache;

    public MarketDataController(MarketDataCache marketDataCache) {
        this.marketDataCache = marketDataCache;
    }

    @GetMapping("/{instrumentKey}")
    public MarketData getMarketData(
            @PathVariable String instrumentKey
    ) {
        return marketDataCache.get(instrumentKey);
    }
}