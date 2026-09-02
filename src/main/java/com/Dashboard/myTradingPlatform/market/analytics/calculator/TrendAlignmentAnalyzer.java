package com.Dashboard.myTradingPlatform.market.analytics.calculator;

import com.Dashboard.myTradingPlatform.market.analytics.model.MultiTimeframeAnalysis;
import com.Dashboard.myTradingPlatform.market.analytics.model.TimeframeAnalysis;
import com.Dashboard.myTradingPlatform.market.analytics.model.TrendAlignment;
import com.Dashboard.myTradingPlatform.market.analytics.model.TrendDirection;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TrendAlignmentAnalyzer {

    /*
     * Minimum number of timeframes required before
     * determining overall market alignment.
     *
     * We currently have:
     *
     * 1d
     * I15
     * I5
     * I1
     *
     * Therefore all four should ideally be available.
     */
    private static final int REQUIRED_TIMEFRAMES = 4;

    public TrendAlignment analyze(
            MultiTimeframeAnalysis analysis) {

        if (analysis == null
                || analysis.analyses() == null
                || analysis.analyses().isEmpty()) {

            return TrendAlignment.INSUFFICIENT_DATA;
        }

        List<TimeframeAnalysis> analyses =
                analysis.analyses();

        /*
         * =====================================================
         * CHECK DATA COMPLETENESS
         * =====================================================
         */
        if (analyses.size() < REQUIRED_TIMEFRAMES) {

            return TrendAlignment.INSUFFICIENT_DATA;
        }

        int bullishCount = 0;
        int bearishCount = 0;
        int neutralCount = 0;

        /*
         * =====================================================
         * COUNT TIMEFRAME DIRECTIONS
         * =====================================================
         */
        for (TimeframeAnalysis timeframeAnalysis
                : analyses) {

            if (timeframeAnalysis == null
                    || timeframeAnalysis.trendDirection() == null) {

                neutralCount++;

                continue;
            }

            TrendDirection direction =
                    timeframeAnalysis.trendDirection();

            if (direction
                    == TrendDirection.BULLISH) {

                bullishCount++;

            } else if (direction
                    == TrendDirection.BEARISH) {

                bearishCount++;

            } else {

                neutralCount++;
            }
        }

        /*
         * =====================================================
         * REQUIRED DATA VALIDATION
         * =====================================================
         *
         * We don't want:
         *
         * 2 bullish + 2 missing
         *
         * to become STRONGLY_BULLISH.
         */
        if (bullishCount
                + bearishCount
                + neutralCount
                < REQUIRED_TIMEFRAMES) {

            return TrendAlignment.INSUFFICIENT_DATA;
        }

        /*
         * =====================================================
         * ALL BULLISH
         * =====================================================
         */
        if (bullishCount == REQUIRED_TIMEFRAMES) {

            return TrendAlignment.STRONGLY_BULLISH;
        }

        /*
         * =====================================================
         * ALL BEARISH
         * =====================================================
         */
        if (bearishCount == REQUIRED_TIMEFRAMES) {

            return TrendAlignment.STRONGLY_BEARISH;
        }

        /*
         * =====================================================
         * BULLISH MAJORITY
         * =====================================================
         */
        if (bullishCount > bearishCount) {

            /*
             * 3 out of 4 bullish.
             */
            if (bullishCount >= 3) {

                return TrendAlignment.BULLISH;
            }

            return TrendAlignment.MIXED_BULLISH;
        }

        /*
         * =====================================================
         * BEARISH MAJORITY
         * =====================================================
         */
        if (bearishCount > bullishCount) {

            /*
             * 3 out of 4 bearish.
             */
            if (bearishCount >= 3) {

                return TrendAlignment.BEARISH;
            }

            return TrendAlignment.MIXED_BEARISH;
        }

        /*
         * =====================================================
         * EQUAL BULLISH / BEARISH
         * =====================================================
         */
        return TrendAlignment.NEUTRAL;
    }
}