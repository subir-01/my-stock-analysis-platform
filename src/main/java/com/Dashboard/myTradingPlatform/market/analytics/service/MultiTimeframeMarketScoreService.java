package com.Dashboard.myTradingPlatform.market.analytics.service;

import com.Dashboard.myTradingPlatform.market.analytics.calculator.WeightedMarketScoreCalculator;
import com.Dashboard.myTradingPlatform.market.analytics.model.MarketScore;
import com.Dashboard.myTradingPlatform.market.analytics.model.MultiTimeframeAnalysis;
import com.Dashboard.myTradingPlatform.market.analytics.model.TimeframeAnalysis;
import com.Dashboard.myTradingPlatform.market.analytics.model.TrendAlignment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class MultiTimeframeMarketScoreService {

    private final MultiTimeframeAnalysisService
            multiTimeframeAnalysisService;

    private final WeightedMarketScoreCalculator
            weightedMarketScoreCalculator;

    public MultiTimeframeMarketScoreService(
            MultiTimeframeAnalysisService multiTimeframeAnalysisService,
            WeightedMarketScoreCalculator weightedMarketScoreCalculator) {

        this.multiTimeframeAnalysisService =
                multiTimeframeAnalysisService;

        this.weightedMarketScoreCalculator =
                weightedMarketScoreCalculator;
    }

    /*
     * =========================================================
     * CALCULATE MARKET SCORE
     * =========================================================
     */
    public MarketScore calculate(
            String instrumentKey) {

        if (instrumentKey == null
                || instrumentKey.isBlank()) {

            throw new IllegalArgumentException(
                    "Instrument key must not be null or blank"
            );
        }

        log.info(
                "Starting weighted market score calculation: instrument={}",
                instrumentKey
        );

        /*
         * =====================================================
         * GET MULTI-TIMEFRAME ANALYSIS
         * =====================================================
         */
        MultiTimeframeAnalysis analysis =
                multiTimeframeAnalysisService.analyze(
                        instrumentKey
                );

        /*
         * =====================================================
         * NULL CHECK
         * =====================================================
         */
        if (analysis == null) {

            log.warn(
                    "Multi-timeframe analysis returned null: instrument={}",
                    instrumentKey
            );

            return createEmptyScore(
                    instrumentKey
            );
        }

        /*
         * =====================================================
         * CHECK DATA SUFFICIENCY
         * =====================================================
         *
         * We must NOT calculate a bullish/bearish score when
         * one or more required timeframes are missing.
         *
         * Example:
         *
         * 1d  -> available
         * I15 -> available
         * I5  -> missing
         * I1  -> available
         *
         * Result:
         *
         * INSUFFICIENT_DATA
         *
         * NOT:
         *
         * bullishPercentage = 100%
         */
        if (analysis.alignment()
                == TrendAlignment.INSUFFICIENT_DATA) {

            log.warn(
                    "Insufficient timeframe data. Market score will not be calculated: instrument={}, availableTimeframes={}",
                    instrumentKey,
                    analysis.analyses() == null
                            ? 0
                            : analysis.analyses().size()
            );

            return createEmptyScore(
                    instrumentKey
            );
        }

        /*
         * =====================================================
         * CHECK ANALYSES
         * =====================================================
         */
        if (analysis.analyses() == null
                || analysis.analyses().isEmpty()) {

            log.warn(
                    "No timeframe analyses available: instrument={}",
                    instrumentKey
            );

            return createEmptyScore(
                    instrumentKey
            );
        }

        /*
         * =====================================================
         * COLLECT TIMEFRAME ANALYSES
         * =====================================================
         */
        List<TimeframeAnalysis> timeframeAnalyses =
                new ArrayList<>(
                        analysis.analyses()
                );

        /*
         * =====================================================
         * CALCULATE WEIGHTED SCORE
         * =====================================================
         */
        MarketScore score =
                weightedMarketScoreCalculator.calculate(
                        instrumentKey,
                        timeframeAnalyses
                );

        log.info(
                "Weighted market score completed: instrument={}, bullishScore={}, bearishScore={}, totalScore={}, bullishPercentage={}, bearishPercentage={}",
                instrumentKey,
                score.bullishScore(),
                score.bearishScore(),
                score.totalScore(),
                score.bullishPercentage(),
                score.bearishPercentage()
        );

        return score;
    }

    /*
     * =========================================================
     * CREATE EMPTY SCORE
     * =========================================================
     */
    private MarketScore createEmptyScore(
            String instrumentKey) {

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
}