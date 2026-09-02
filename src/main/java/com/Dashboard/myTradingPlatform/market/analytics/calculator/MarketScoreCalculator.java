package com.Dashboard.myTradingPlatform.market.analytics.calculator;

import com.Dashboard.myTradingPlatform.market.analytics.model.MarketAnalysis;
import com.Dashboard.myTradingPlatform.market.analytics.model.MarketScore;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class MarketScoreCalculator {

    /*
     * =========================================================
     * INDICATOR WEIGHTS
     * =========================================================
     *
     * Current phase:
     *
     * Trend       -> 40
     * Momentum    -> 20
     * VWAP        -> 20
     * Price       -> 20
     *
     * Total       -> 100
     *
     * Later we will add:
     *
     * Volume
     * OI
     * PCR
     * Market breadth
     * Candlestick confirmation
     * Support / Resistance
     * Market regime
     */
    private static final int TREND_WEIGHT = 40;
    private static final int MOMENTUM_WEIGHT = 20;
    private static final int VWAP_WEIGHT = 20;
    private static final int PRICE_WEIGHT = 20;

    public MarketScore calculate(
            String instrumentKey,
            List<MarketAnalysis> analyses) {

        if (analyses == null
                || analyses.isEmpty()) {

            return new MarketScore(
                    instrumentKey,
                    Instant.now(),
                    0,
                    0,
                    0,
                    0.0,
                    0.0
            );
        }

        int bullishScore = 0;
        int bearishScore = 0;

        /*
         * =====================================================
         * PROCESS ALL AVAILABLE TIMEFRAMES
         * =====================================================
         */
        for (MarketAnalysis analysis : analyses) {

            if (analysis == null
                    || analysis.price() == null) {

                continue;
            }

            /*
             * =================================================
             * TREND
             * =================================================
             *
             * EMA20 vs EMA50.
             */
            if (analysis.ema20() != null
                    && analysis.ema50() != null) {

                if (analysis.ema20()
                        .compareTo(analysis.ema50()) > 0) {

                    bullishScore += TREND_WEIGHT;

                } else if (analysis.ema20()
                        .compareTo(analysis.ema50()) < 0) {

                    bearishScore += TREND_WEIGHT;
                }
            }

            /*
             * =================================================
             * MOMENTUM
             * =================================================
             *
             * RSI:
             *
             * >= 55 -> bullish
             * <= 45 -> bearish
             */
            if (analysis.rsi14() != null) {

                if (analysis.rsi14()
                        .doubleValue() >= 55) {

                    bullishScore += MOMENTUM_WEIGHT;

                } else if (analysis.rsi14()
                        .doubleValue() <= 45) {

                    bearishScore += MOMENTUM_WEIGHT;
                }
            }

            /*
             * =================================================
             * VWAP
             * =================================================
             */
            if (analysis.vwap() != null) {

                if (analysis.price()
                        .compareTo(analysis.vwap()) > 0) {

                    bullishScore += VWAP_WEIGHT;

                } else if (analysis.price()
                        .compareTo(analysis.vwap()) < 0) {

                    bearishScore += VWAP_WEIGHT;
                }
            }

            /*
             * =================================================
             * PRICE VS SMA20
             * =================================================
             */
            if (analysis.sma20() != null) {

                if (analysis.price()
                        .compareTo(analysis.sma20()) > 0) {

                    bullishScore += PRICE_WEIGHT;

                } else if (analysis.price()
                        .compareTo(analysis.sma20()) < 0) {

                    bearishScore += PRICE_WEIGHT;
                }
            }
        }

        /*
         * =====================================================
         * TOTAL
         * =====================================================
         */
        int totalScore =
                bullishScore + bearishScore;

        double bullishPercentage =
                calculatePercentage(
                        bullishScore,
                        totalScore
                );

        double bearishPercentage =
                calculatePercentage(
                        bearishScore,
                        totalScore
                );

        Instant timestamp =
                analyses.stream()
                        .map(MarketAnalysis::timestamp)
                        .filter(java.util.Objects::nonNull)
                        .max(Instant::compareTo)
                        .orElse(Instant.now());

        return new MarketScore(
                instrumentKey,
                timestamp,
                bullishScore,
                bearishScore,
                totalScore,
                bullishPercentage,
                bearishPercentage
        );
    }

    private double calculatePercentage(
            int score,
            int total) {

        if (total == 0) {
            return 0.0;
        }

        return (score * 100.0) / total;
    }
}