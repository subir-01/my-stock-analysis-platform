package com.Dashboard.myTradingPlatform.market.analytics.service;

import com.Dashboard.myTradingPlatform.market.analytics.calculator.EmaCalculator;
import com.Dashboard.myTradingPlatform.market.analytics.calculator.ResistanceCalculator;
import com.Dashboard.myTradingPlatform.market.analytics.calculator.RsiCalculator;
import com.Dashboard.myTradingPlatform.market.analytics.calculator.SmaCalculator;
import com.Dashboard.myTradingPlatform.market.analytics.calculator.SupportCalculator;
import com.Dashboard.myTradingPlatform.market.analytics.calculator.VwapCalculator;
import com.Dashboard.myTradingPlatform.market.analytics.model.MarketAnalysis;
import com.Dashboard.myTradingPlatform.market.cache.MarketCandleCache;
import com.Dashboard.myTradingPlatform.market.model.MarketCandle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class MarketAnalyticsService {

    private static final int ANALYSIS_CANDLE_COUNT = 100;

    private final MarketCandleCache marketCandleCache;
    private final SmaCalculator smaCalculator;
    private final EmaCalculator emaCalculator;
    private final RsiCalculator rsiCalculator;
    private final VwapCalculator vwapCalculator;
    private final SupportCalculator supportCalculator;
    private final ResistanceCalculator resistanceCalculator;

    public MarketAnalyticsService(
            MarketCandleCache marketCandleCache,
            SmaCalculator smaCalculator,
            EmaCalculator emaCalculator,
            RsiCalculator rsiCalculator,
            VwapCalculator vwapCalculator,
            SupportCalculator supportCalculator,
            ResistanceCalculator resistanceCalculator) {

        this.marketCandleCache = marketCandleCache;
        this.smaCalculator = smaCalculator;
        this.emaCalculator = emaCalculator;
        this.rsiCalculator = rsiCalculator;
        this.vwapCalculator = vwapCalculator;
        this.supportCalculator = supportCalculator;
        this.resistanceCalculator = resistanceCalculator;
    }

    /*
     * =========================================================
     * MARKET ANALYSIS
     * =========================================================
     */
    public MarketAnalysis analyze(
            String instrumentKey,
            String timeframe) {

        if (instrumentKey == null
                || instrumentKey.isBlank()) {

            log.warn(
                    "Cannot analyze market: instrumentKey is empty"
            );

            return null;
        }

        if (timeframe == null
                || timeframe.isBlank()) {

            log.warn(
                    "Cannot analyze market: timeframe is empty"
            );

            return null;
        }

        /*
         * Get the latest candles from memory.
         *
         * MarketCandleCache returns candles in:
         *
         * oldest -> newest
         *
         * This is required by EMA/RSI calculations.
         */
        List<MarketCandle> candles =
                marketCandleCache.getLastCandles(
                        instrumentKey,
                        timeframe,
                        ANALYSIS_CANDLE_COUNT
                );

        if (candles == null
                || candles.isEmpty()) {

            log.warn(
                    "No candles available for analysis: instrument={}, timeframe={}",
                    instrumentKey,
                    timeframe
            );

            return null;
        }

        /*
         * Remove null candles defensively.
         */
        candles =
                candles.stream()
                        .filter(candle -> candle != null)
                        .toList();

        if (candles.isEmpty()) {

            log.warn(
                    "No valid candles available for analysis: instrument={}, timeframe={}",
                    instrumentKey,
                    timeframe
            );

            return null;
        }

        /*
         * =====================================================
         * LATEST CANDLE
         * =====================================================
         */
        MarketCandle latest =
                candles.get(
                        candles.size() - 1
                );

        if (latest.close() == null) {

            log.warn(
                    "Latest candle has no close price: instrument={}, timeframe={}",
                    instrumentKey,
                    timeframe
            );

            return null;
        }

        BigDecimal currentPrice =
                latest.close();

        /*
         * =====================================================
         * SMA 20
         * =====================================================
         */
        BigDecimal sma20 =
                smaCalculator.calculate(
                        candles,
                        20
                );

        /*
         * =====================================================
         * EMA 20
         * =====================================================
         */
        BigDecimal ema20 =
                emaCalculator.calculate(
                        candles,
                        20
                );

        /*
         * =====================================================
         * EMA 50
         * =====================================================
         */
        BigDecimal ema50 =
                emaCalculator.calculate(
                        candles,
                        50
                );

        /*
         * =====================================================
         * RSI 14
         * =====================================================
         */
        BigDecimal rsi14 =
                rsiCalculator.calculate(
                        candles,
                        14
                );

        /*
         * =====================================================
         * VWAP
         * =====================================================
         */
        BigDecimal vwap =
                vwapCalculator.calculate(
                        candles
                );

        /*
         * =====================================================
         * SUPPORT
         * =====================================================
         */
        BigDecimal support =
                supportCalculator.calculate(
                        candles,
                        currentPrice
                );

        /*
         * =====================================================
         * RESISTANCE
         * =====================================================
         */
        BigDecimal resistance =
                resistanceCalculator.calculate(
                        candles,
                        currentPrice
                );

        /*
         * =====================================================
         * BUILD ANALYSIS
         * =====================================================
         */
        MarketAnalysis analysis =
                new MarketAnalysis(
                        instrumentKey,
                        timeframe,
                        latest.timestamp(),
                        currentPrice,
                        sma20,
                        ema20,
                        ema50,
                        rsi14,
                        vwap,
                        support,
                        resistance
                );

        log.info(
                "Market analysis completed: instrument={}, timeframe={}, price={}, SMA20={}, EMA20={}, EMA50={}, RSI14={}, VWAP={}, support={}, resistance={}",
                instrumentKey,
                timeframe,
                currentPrice,
                sma20,
                ema20,
                ema50,
                rsi14,
                vwap,
                support,
                resistance
        );

        return analysis;
    }

    /*
     * =========================================================
     * CHECK CACHE AVAILABILITY
     * =========================================================
     */
    public boolean hasEnoughCandles(
            String instrumentKey,
            String timeframe,
            int requiredCount) {

        if (requiredCount <= 0) {
            return false;
        }

        int count =
                marketCandleCache.getCandleCount(
                        instrumentKey,
                        timeframe
                );

        return count >= requiredCount;
    }

    /*
     * =========================================================
     * GET CANDLE COUNT
     * =========================================================
     */
    public int getCandleCount(
            String instrumentKey,
            String timeframe) {

        return marketCandleCache.getCandleCount(
                instrumentKey,
                timeframe
        );
    }
}