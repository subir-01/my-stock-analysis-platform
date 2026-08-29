package com.Dashboard.myTradingPlatform.market.analytics.controller;

import com.Dashboard.myTradingPlatform.market.analytics.model.MarketAnalysis;
import com.Dashboard.myTradingPlatform.market.analytics.service.MarketAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/market/analysis")
public class MarketAnalyticsController {

    private final MarketAnalyticsService marketAnalyticsService;

    public MarketAnalyticsController(
            MarketAnalyticsService marketAnalyticsService) {

        this.marketAnalyticsService =
                marketAnalyticsService;
    }

    @GetMapping
    public ResponseEntity<MarketAnalysis> analyze(
            @RequestParam String instrumentKey,
            @RequestParam String timeframe) {

        MarketAnalysis analysis =
                marketAnalyticsService.analyze(
                        instrumentKey,
                        timeframe
                );

        if (analysis == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(analysis);
    }
}