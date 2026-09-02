package com.Dashboard.myTradingPlatform.market.analytics.service;

import com.Dashboard.myTradingPlatform.market.analytics.calculator.LiquidityCalculator;
import com.Dashboard.myTradingPlatform.market.analytics.calculator.MarketStateCalculator;
import com.Dashboard.myTradingPlatform.market.analytics.calculator.PriceActionCalculator;
import com.Dashboard.myTradingPlatform.market.analytics.calculator.VolumeCalculator;
import com.Dashboard.myTradingPlatform.market.analytics.model.CandlePatternAnalysis;
import com.Dashboard.myTradingPlatform.market.analytics.model.LiquidityAnalysis;
import com.Dashboard.myTradingPlatform.market.analytics.model.MarketAnalysis;
import com.Dashboard.myTradingPlatform.market.analytics.model.MarketState;
import com.Dashboard.myTradingPlatform.market.analytics.model.MultiTimeframeAnalysis;
import com.Dashboard.myTradingPlatform.market.analytics.model.PriceActionAnalysis;
import com.Dashboard.myTradingPlatform.market.analytics.model.TimeframeAnalysis;
import com.Dashboard.myTradingPlatform.market.analytics.model.TrendAlignment;
import com.Dashboard.myTradingPlatform.market.analytics.model.TrendDirection;
import com.Dashboard.myTradingPlatform.market.analytics.model.VolumeAnalysis;
import com.Dashboard.myTradingPlatform.market.cache.MarketCandleCache;
import com.Dashboard.myTradingPlatform.market.model.MarketCandle;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class MultiTimeframeAnalysisService {

    private static final List<String> TIMEFRAMES =
            List.of(
                    "1d",
                    "I15",
                    "I5",
                    "I1"
            );

    private final MarketAnalyticsService marketAnalyticsService;

    private final CandlePatternService candlePatternService;

    private final MarketStateCalculator marketStateCalculator;

    private final PriceActionCalculator priceActionCalculator;

    private final VolumeCalculator volumeCalculator;

    private final LiquidityCalculator liquidityCalculator;

    private final MarketCandleCache marketCandleCache;

    public MultiTimeframeAnalysisService(
            MarketAnalyticsService marketAnalyticsService,
            CandlePatternService candlePatternService,
            MarketStateCalculator marketStateCalculator,
            PriceActionCalculator priceActionCalculator,
            VolumeCalculator volumeCalculator,
            LiquidityCalculator liquidityCalculator,
            MarketCandleCache marketCandleCache) {

        this.marketAnalyticsService =
                marketAnalyticsService;

        this.candlePatternService =
                candlePatternService;

        this.marketStateCalculator =
                marketStateCalculator;

        this.priceActionCalculator =
                priceActionCalculator;

        this.volumeCalculator =
                volumeCalculator;

        this.liquidityCalculator =
                liquidityCalculator;

        this.marketCandleCache =
                marketCandleCache;
    }

    /*
     * =========================================================
     * MAIN ANALYSIS
     * =========================================================
     */
    public MultiTimeframeAnalysis analyze(
            String instrumentKey) {

        List<TimeframeAnalysis> analyses =
                new ArrayList<>();

        for (String timeframe :
                TIMEFRAMES) {

            /*
             * =================================================
             * MARKET ANALYSIS
             * =================================================
             */
            MarketAnalysis analysis =
                    marketAnalyticsService.analyze(
                            instrumentKey,
                            timeframe
                    );

            if (analysis == null) {
                continue;
            }

            /*
             * =================================================
             * TREND
             * =================================================
             */
            TrendResult trend =
                    calculateTrend(
                            analysis
                    );

            /*
             * =================================================
             * CANDLE PATTERN
             * =================================================
             */
            CandlePatternAnalysis candlePattern =
                    candlePatternService.analyze(
                            instrumentKey,
                            timeframe
                    );

            /*
             * =================================================
             * MARKET STATE
             * =================================================
             */
            MarketStateCalculator.MarketStateResult
                    stateResult =
                    marketStateCalculator.calculate(
                            analysis
                    );

            MarketState marketState =
                    new MarketState(
                            stateResult.regime(),
                            stateResult.momentum(),
                            stateResult.momentumCondition()
                    );

            /*
             * =================================================
             * RECENT CANDLES
             *
             * Used for:
             * - Price Action
             * - Volume
             *
             * =================================================
             */
            List<MarketCandle> candles =
                    marketCandleCache.getLastCandles(
                            instrumentKey,
                            timeframe,
                            20
                    );

            /*
             * =================================================
             * PRICE ACTION
             * =================================================
             */
            PriceActionAnalysis priceAction =
                    priceActionCalculator.calculate(
                            analysis,
                            candles
                    );

            /*
             * =================================================
             * VOLUME
             * =================================================
             */
            VolumeAnalysis volumeAnalysis =
                    volumeCalculator.calculate(
                            timeframe,
                            candles
                    );

            /*
             * =================================================
             * LIQUIDITY
             *
             * Liquidity requires a larger historical
             * window than price action.
             *
             * =================================================
             */
            List<MarketCandle> liquidityCandles =
                    marketCandleCache.getLastCandles(
                            instrumentKey,
                            timeframe,
                            200
                    );

            LiquidityAnalysis liquidity =
                    liquidityCalculator.calculate(
                            liquidityCandles
                    );

            /*
             * =================================================
             * BUILD TIMEFRAME ANALYSIS
             * =================================================
             */
            analyses.add(
                    new TimeframeAnalysis(
                            analysis,
                            trend.direction(),
                            trend.bullishScore(),
                            trend.bearishScore(),
                            candlePattern,
                            marketState,
                            priceAction,
                            volumeAnalysis,
                            liquidity
                    )
            );
        }

        /*
         * =====================================================
         * ALIGNMENT
         * =====================================================
         */
        TrendAlignment alignment =
                calculateAlignment(
                        analyses
                );

        return new MultiTimeframeAnalysis(
                instrumentKey,
                Instant.now(),
                analyses,
                alignment
        );
    }

    /*
     * =========================================================
     * TREND CALCULATION
     * =========================================================
     */
    private TrendResult calculateTrend(
            MarketAnalysis analysis) {

        int bullish = 0;

        int bearish = 0;

        /*
         * Price vs SMA20
         */
        if (analysis.price() != null
                && analysis.sma20() != null) {

            int comparison =
                    analysis.price()
                            .compareTo(
                                    analysis.sma20()
                            );

            if (comparison > 0) {

                bullish++;

            } else if (comparison < 0) {

                bearish++;
            }
        }

        /*
         * Price vs EMA20
         */
        if (analysis.price() != null
                && analysis.ema20() != null) {

            int comparison =
                    analysis.price()
                            .compareTo(
                                    analysis.ema20()
                            );

            if (comparison > 0) {

                bullish++;

            } else if (comparison < 0) {

                bearish++;
            }
        }

        /*
         * EMA20 vs EMA50
         */
        if (analysis.ema20() != null
                && analysis.ema50() != null) {

            int comparison =
                    analysis.ema20()
                            .compareTo(
                                    analysis.ema50()
                            );

            if (comparison > 0) {

                bullish++;

            } else if (comparison < 0) {

                bearish++;
            }
        }

        /*
         * Price vs VWAP
         */
        if (analysis.price() != null
                && analysis.vwap() != null) {

            int comparison =
                    analysis.price()
                            .compareTo(
                                    analysis.vwap()
                            );

            if (comparison > 0) {

                bullish++;

            } else if (comparison < 0) {

                bearish++;
            }
        }

        /*
         * RSI vs 50
         */
        if (analysis.rsi14() != null) {

            int comparison =
                    analysis.rsi14()
                            .compareTo(
                                    BigDecimal.valueOf(50)
                            );

            if (comparison > 0) {

                bullish++;

            } else if (comparison < 0) {

                bearish++;
            }
        }

        TrendDirection direction;

        if (bullish > bearish) {

            direction =
                    TrendDirection.BULLISH;

        } else if (bearish > bullish) {

            direction =
                    TrendDirection.BEARISH;

        } else {

            direction =
                    TrendDirection.NEUTRAL;
        }

        return new TrendResult(
                direction,
                bullish,
                bearish
        );
    }

    /*
     * =========================================================
     * ALIGNMENT
     * =========================================================
     */
    private TrendAlignment calculateAlignment(
            List<TimeframeAnalysis> analyses) {

        if (analyses.size() < TIMEFRAMES.size()) {

            return TrendAlignment.INSUFFICIENT_DATA;
        }

        int bullish = 0;

        int bearish = 0;

        for (TimeframeAnalysis analysis :
                analyses) {

            if (analysis == null) {
                continue;
            }

            if (analysis.trendDirection()
                    == TrendDirection.BULLISH) {

                bullish++;

            } else if (analysis.trendDirection()
                    == TrendDirection.BEARISH) {

                bearish++;
            }
        }

        if (bullish == analyses.size()) {

            return TrendAlignment.STRONGLY_BULLISH;
        }

        if (bearish == analyses.size()) {

            return TrendAlignment.STRONGLY_BEARISH;
        }

        if (bullish >= 3) {

            return TrendAlignment.BULLISH;
        }

        if (bearish >= 3) {

            return TrendAlignment.BEARISH;
        }

        if (bullish > bearish) {

            return TrendAlignment.MIXED_BULLISH;
        }

        if (bearish > bullish) {

            return TrendAlignment.MIXED_BEARISH;
        }

        return TrendAlignment.NEUTRAL;
    }

    /*
     * =========================================================
     * PUBLIC TIMEFRAMES
     * =========================================================
     */
    public List<String> getTimeframes() {

        return TIMEFRAMES;
    }

    /*
     * =========================================================
     * INTERNAL TREND RESULT
     * =========================================================
     */
    private record TrendResult(
            TrendDirection direction,
            int bullishScore,
            int bearishScore
    ) {
    }
}