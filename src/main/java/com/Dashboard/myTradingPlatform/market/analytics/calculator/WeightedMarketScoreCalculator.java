package com.Dashboard.myTradingPlatform.market.analytics.calculator;

import com.Dashboard.myTradingPlatform.market.analytics.model.MarketAnalysis;
import com.Dashboard.myTradingPlatform.market.analytics.model.MarketScore;
import com.Dashboard.myTradingPlatform.market.analytics.model.TimeframeAnalysis;
import com.Dashboard.myTradingPlatform.market.analytics.model.TrendDirection;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Component
public class WeightedMarketScoreCalculator {

    /*
     * =========================================================
     * TIMEFRAME WEIGHTS
     * =========================================================
     *
     * Higher timeframe = higher importance.
     *
     * 1D  -> 40%
     * I15 -> 30%
     * I5  -> 20%
     * I1  -> 10%
     *
     * Total = 100%
     */
    private static final int DAILY_WEIGHT = 40;
    private static final int FIFTEEN_MIN_WEIGHT = 30;
    private static final int FIVE_MIN_WEIGHT = 20;
    private static final int ONE_MIN_WEIGHT = 10;

    public MarketScore calculate(
            String instrumentKey,
            List<TimeframeAnalysis> analyses) {

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

        double bullishScore = 0.0;
        double bearishScore = 0.0;

        /*
         * =====================================================
         * PROCESS EACH TIMEFRAME
         * =====================================================
         */
        for (TimeframeAnalysis timeframeAnalysis
                : analyses) {

            if (timeframeAnalysis == null
                    || timeframeAnalysis.analysis() == null) {

                continue;
            }

            MarketAnalysis analysis =
                    timeframeAnalysis.analysis();

            String timeframe =
                    analysis.timeframe();

            int weight =
                    getWeight(timeframe);

            if (weight == 0) {
                continue;
            }

            /*
             * =================================================
             * DETERMINE TIMEFRAME DIRECTION
             * =================================================
             */
            TrendDirection direction =
                    timeframeAnalysis.trendDirection();

            if (direction
                    == TrendDirection.BULLISH) {

                bullishScore += weight;

            } else if (direction
                    == TrendDirection.BEARISH) {

                bearishScore += weight;
            }
        }

        /*
         * =====================================================
         * TOTAL SCORE
         * =====================================================
         */
        double totalScore =
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
                        .map(TimeframeAnalysis::analysis)
                        .filter(Objects::nonNull)
                        .map(MarketAnalysis::timestamp)
                        .filter(Objects::nonNull)
                        .max(Instant::compareTo)
                        .orElse(Instant.now());

        return new MarketScore(
                instrumentKey,
                timestamp,
                (int) Math.round(bullishScore),
                (int) Math.round(bearishScore),
                (int) Math.round(totalScore),
                bullishPercentage,
                bearishPercentage
        );
    }

    /*
     * =========================================================
     * GET TIMEFRAME WEIGHT
     * =========================================================
     */
    private int getWeight(
            String timeframe) {

        if (timeframe == null) {
            return 0;
        }

        return switch (
                timeframe.toUpperCase()
                ) {

            case "1D", "D1" ->
                    DAILY_WEIGHT;

            case "I15" ->
                    FIFTEEN_MIN_WEIGHT;

            case "I5" ->
                    FIVE_MIN_WEIGHT;

            case "I1" ->
                    ONE_MIN_WEIGHT;

            default ->
                    0;
        };
    }

    /*
     * =========================================================
     * PERCENTAGE
     * =========================================================
     */
    private double calculatePercentage(
            double score,
            double total) {

        if (total == 0.0) {
            return 0.0;
        }

        return (score * 100.0) / total;
    }
}