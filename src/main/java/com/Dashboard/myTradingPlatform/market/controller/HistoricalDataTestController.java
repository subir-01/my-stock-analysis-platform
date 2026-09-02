package com.Dashboard.myTradingPlatform.market.controller;

import com.Dashboard.myTradingPlatform.market.analytics.model.MarketAnalysis;
import com.Dashboard.myTradingPlatform.market.analytics.service.MarketAnalyticsService;
import com.Dashboard.myTradingPlatform.market.cache.MarketCandleCache;
import com.Dashboard.myTradingPlatform.market.model.MarketCandle;
import com.Dashboard.myTradingPlatform.market.service.HistoricalDataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class HistoricalDataTestController {

    private static final String INSTRUMENT_KEY =
            "NSE_EQ|INE002A01018";

    private static final String TIMEFRAME =
            "I1";

    private final HistoricalDataService historicalDataService;
    private final MarketCandleCache marketCandleCache;
    private final MarketAnalyticsService marketAnalyticsService;

    public HistoricalDataTestController(
            HistoricalDataService historicalDataService,
            MarketCandleCache marketCandleCache,
            MarketAnalyticsService marketAnalyticsService) {

        this.historicalDataService =
                historicalDataService;

        this.marketCandleCache =
                marketCandleCache;

        this.marketAnalyticsService =
                marketAnalyticsService;
    }

    /*
     * =========================================================
     * TEST HISTORICAL DATA
     * =========================================================
     *
     * Downloads historical I1 candles and processes them.
     *
     * NOTE:
     * The interval parameter is no longer required.
     */
    @GetMapping("/api/test/historical")
    public String testHistoricalData() {

        historicalDataService.loadHistoricalData(
                INSTRUMENT_KEY,
                TIMEFRAME,
                "2026-08-01",
                "2026-08-27"
        );

        int count =
                marketCandleCache.getCandleCount(
                        INSTRUMENT_KEY,
                        TIMEFRAME
                );

        return "Historical candles loaded into cache: "
                + count;
    }

    /*
     * =========================================================
     * GET LAST 100 CANDLES
     * =========================================================
     *
     * Returned in chronological order:
     *
     * oldest -> newest
     */
    @GetMapping("/api/test/candles")
    public List<MarketCandle> getLastCandles() {

        return marketCandleCache.getLastCandles(
                INSTRUMENT_KEY,
                TIMEFRAME,
                100
        );
    }

    /*
     * =========================================================
     * GET LATEST CANDLE
     * =========================================================
     */
    @GetMapping("/api/test/latest-candle")
    public MarketCandle getLatestCandle() {

        return marketCandleCache.getLatest(
                INSTRUMENT_KEY,
                TIMEFRAME
        );
    }

    /*
     * =========================================================
     * GET CURRENT LIVE CANDLE
     * =========================================================
     *
     * Useful for verifying that the WebSocket is continuously
     * updating the currently forming candle.
     */
    @GetMapping("/api/test/current-candle")
    public MarketCandle getCurrentCandle() {

        return marketCandleCache.getLatest(
                INSTRUMENT_KEY,
                TIMEFRAME
        );
    }

    /*
     * =========================================================
     * GET MARKET ANALYSIS
     * =========================================================
     *
     * Analytics reads the latest candles directly from the
     * in-memory cache.
     */
    @GetMapping("/api/test/analysis")
    public MarketAnalysis testAnalysis() {

        return marketAnalyticsService.analyze(
                INSTRUMENT_KEY,
                TIMEFRAME
        );
    }

    /*
     * =========================================================
     * GET CACHE COUNT
     * =========================================================
     */
    @GetMapping("/api/test/cache-count")
    public String getCacheCount() {

        int count =
                marketCandleCache.getCandleCount(
                        INSTRUMENT_KEY,
                        TIMEFRAME
                );

        return "Cached candles: " + count;
    }

    /*
     * =========================================================
     * CHECK CACHE
     * =========================================================
     */
    @GetMapping("/api/test/cache-exists")
    public boolean cacheExists() {

        return marketCandleCache.contains(
                INSTRUMENT_KEY,
                TIMEFRAME
        );
    }
}