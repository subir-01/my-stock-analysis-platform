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

    private static final String DEFAULT_INSTRUMENT =
            "NSE_EQ|INE002A01018";

    private static final String DEFAULT_TIMEFRAME =
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
     * Example:
     *
     * GET /api/test/historical
     *
     * Downloads I1 candles from Upstox and stores them in
     * database + cache.
     */
    @GetMapping("/api/test/historical")
    public String testHistoricalData() {

        historicalDataService.loadHistoricalData(
                DEFAULT_INSTRUMENT,
                DEFAULT_TIMEFRAME,
                "2026-08-01",
                "2026-08-27"
        );

        int count =
                marketCandleCache.getCandleCount(
                        DEFAULT_INSTRUMENT,
                        DEFAULT_TIMEFRAME
                );

        return "Historical candles loaded into cache: "
                + count;
    }

    /*
     * =========================================================
     * GET LAST 100 CANDLES
     * =========================================================
     *
     * GET /api/test/candles
     */
    @GetMapping("/api/test/candles")
    public List<MarketCandle> getLastCandles() {

        return marketCandleCache.getLastCandles(
                DEFAULT_INSTRUMENT,
                DEFAULT_TIMEFRAME,
                100
        );
    }

    /*
     * =========================================================
     * GET LATEST CANDLE
     * =========================================================
     *
     * GET /api/test/latest-candle
     */
    @GetMapping("/api/test/latest-candle")
    public MarketCandle getLatestCandle() {

        return marketCandleCache.getLatest(
                DEFAULT_INSTRUMENT,
                DEFAULT_TIMEFRAME
        );
    }

    /*
     * =========================================================
     * GET CANDLE COUNT
     * =========================================================
     *
     * GET /api/test/candle-count
     */
    @GetMapping("/api/test/candle-count")
    public String getCandleCount() {

        int count =
                marketCandleCache.getCandleCount(
                        DEFAULT_INSTRUMENT,
                        DEFAULT_TIMEFRAME
                );

        return "Cached candle count: " + count;
    }

    /*
     * =========================================================
     * CHECK CACHE
     * =========================================================
     *
     * GET /api/test/cache-status
     */
    @GetMapping("/api/test/cache-status")
    public String getCacheStatus() {

        boolean contains =
                marketCandleCache.contains(
                        DEFAULT_INSTRUMENT,
                        DEFAULT_TIMEFRAME
                );

        if (!contains) {

            return "Cache is empty for instrument="
                    + DEFAULT_INSTRUMENT
                    + ", timeframe="
                    + DEFAULT_TIMEFRAME;
        }

        int count =
                marketCandleCache.getCandleCount(
                        DEFAULT_INSTRUMENT,
                        DEFAULT_TIMEFRAME
                );

        MarketCandle latest =
                marketCandleCache.getLatest(
                        DEFAULT_INSTRUMENT,
                        DEFAULT_TIMEFRAME
                );

        return "Cache contains candles: "
                + count
                + ", latestTimestamp="
                + (latest != null
                ? latest.timestamp()
                : null);
    }

    /*
     * =========================================================
     * MARKET ANALYSIS
     * =========================================================
     *
     * GET /api/test/analysis
     *
     * Returns:
     *
     * price
     * SMA20
     * EMA20
     * EMA50
     * RSI14
     * VWAP
     * support
     * resistance
     */
    @GetMapping("/api/test/analysis")
    public MarketAnalysis testAnalysis() {

        return marketAnalyticsService.analyze(
                DEFAULT_INSTRUMENT,
                DEFAULT_TIMEFRAME
        );
    }

    /*
     * =========================================================
     * ANALYSIS STATUS
     * =========================================================
     *
     * Useful for quickly checking whether enough candles
     * are available for the indicators.
     *
     * GET /api/test/analysis-status
     */
    @GetMapping("/api/test/analysis-status")
    public String analysisStatus() {

        int count =
                marketCandleCache.getCandleCount(
                        DEFAULT_INSTRUMENT,
                        DEFAULT_TIMEFRAME
                );

        if (count < 50) {

            return "Not enough candles for full analysis. "
                    + "Available="
                    + count
                    + ", required=50";
        }

        MarketAnalysis analysis =
                marketAnalyticsService.analyze(
                        DEFAULT_INSTRUMENT,
                        DEFAULT_TIMEFRAME
                );

        if (analysis == null) {

            return "Market analysis is not available";
        }

        return "Market analysis available: "
                + "price="
                + analysis.price()
                + ", SMA20="
                + analysis.sma20()
                + ", EMA20="
                + analysis.ema20()
                + ", EMA50="
                + analysis.ema50()
                + ", RSI14="
                + analysis.rsi14()
                + ", VWAP="
                + analysis.vwap()
                + ", support="
                + analysis.support()
                + ", resistance="
                + analysis.resistance();
    }
}