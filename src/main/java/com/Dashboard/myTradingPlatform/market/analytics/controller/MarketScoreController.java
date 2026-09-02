package com.Dashboard.myTradingPlatform.market.analytics.controller;

import com.Dashboard.myTradingPlatform.market.analytics.model.MarketScore;
import com.Dashboard.myTradingPlatform.market.analytics.service.MultiTimeframeMarketScoreService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MarketScoreController {

    private static final String DEFAULT_INSTRUMENT_KEY =
            "NSE_EQ|INE002A01018";

    private final MultiTimeframeMarketScoreService
            marketScoreService;

    public MarketScoreController(
            MultiTimeframeMarketScoreService marketScoreService) {

        this.marketScoreService =
                marketScoreService;
    }

    /*
     * =========================================================
     * MARKET SCORE
     * =========================================================
     *
     * Example:
     *
     * GET /api/market/score
     *
     * Or:
     *
     * GET /api/market/score
     * ?instrumentKey=NSE_EQ|INE002A01018
     */
    @GetMapping("/api/market/score")
    public MarketScore getMarketScore(
            @RequestParam(
                    required = false
            )
            String instrumentKey) {

        String resolvedInstrumentKey =
                resolveInstrumentKey(
                        instrumentKey
                );

        return marketScoreService.calculate(
                resolvedInstrumentKey
        );
    }

    /*
     * =========================================================
     * RESOLVE INSTRUMENT
     * =========================================================
     */
    private String resolveInstrumentKey(
            String instrumentKey) {

        if (instrumentKey == null
                || instrumentKey.isBlank()) {

            return DEFAULT_INSTRUMENT_KEY;
        }

        return instrumentKey;
    }
}