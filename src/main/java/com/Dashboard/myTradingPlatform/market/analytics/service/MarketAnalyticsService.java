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

    private static final int SMA_PERIOD = 20;

    private static final int EMA_SHORT_PERIOD = 20;

    private static final int EMA_LONG_PERIOD = 50;

    private static final int RSI_PERIOD = 14;

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
     *
     * All calculations are performed using the latest candles
     * available in the in-memory MarketCandleCache.
     *
     * Candle order:
     *
     * oldest -> newest
     */
    public MarketAnalysis analyze(
            String instrumentKey,
            String timeframe) {

        validateRequest(
                instrumentKey,
                timeframe
        );

        /*
         * =====================================================
         * GET LATEST CANDLES FROM CACHE
         * =====================================================
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
                    "Latest candle has no close price: instrument={}, timeframe={}, timestamp={}",
                    instrumentKey,
                    timeframe,
                    latest.timestamp()
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
                        SMA_PERIOD
                );

        /*
         * =====================================================
         * EMA 20
         * =====================================================
         */
        BigDecimal ema20 =
                emaCalculator.calculate(
                        candles,
                        EMA_SHORT_PERIOD
                );

        /*
         * =====================================================
         * EMA 50
         * =====================================================
         */
        BigDecimal ema50 =
                emaCalculator.calculate(
                        candles,
                        EMA_LONG_PERIOD
                );

        /*
         * =====================================================
         * RSI 14
         * =====================================================
         */
        BigDecimal rsi14 =
                rsiCalculator.calculate(
                        candles,
                        RSI_PERIOD
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
     * VALIDATE REQUEST
     * =========================================================
     */
    private void validateRequest(
            String instrumentKey,
            String timeframe) {

        if (instrumentKey == null
                || instrumentKey.isBlank()) {

            throw new IllegalArgumentException(
                    "Instrument key must not be null or blank"
            );
        }

        if (timeframe == null
                || timeframe.isBlank()) {

            throw new IllegalArgumentException(
                    "Timeframe must not be null or blank"
            );
        }
    }
}