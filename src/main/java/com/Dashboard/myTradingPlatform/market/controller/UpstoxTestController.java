package com.Dashboard.myTradingPlatform.market.controller;


import com.Dashboard.myTradingPlatform.market.model.MarketData;
import com.Dashboard.myTradingPlatform.market.provider.MarketDataProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UpstoxTestController {

    private final MarketDataProvider marketDataProvider;

    public UpstoxTestController(MarketDataProvider marketDataProvider) {
        this.marketDataProvider = marketDataProvider;
    }

    @GetMapping("/api/test/upstox/ltp")
    public MarketData getLtp(@RequestParam String instrumentKey) {

        return marketDataProvider.getLtp(instrumentKey);
    }
}