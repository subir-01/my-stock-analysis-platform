package com.Dashboard.myTradingPlatform.market.analytics.calculator;

import com.Dashboard.myTradingPlatform.market.analytics.model.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MarketSignalAnalyzer {

    private final SupportResistanceCalculator
            supportResistanceCalculator;

    public MarketSignalAnalyzer(
            SupportResistanceCalculator supportResistanceCalculator) {

        this.supportResistanceCalculator =
                supportResistanceCalculator;
    }

    /*
     * =========================================================
     * MAIN ANALYSIS
     * =========================================================
     */
    public MarketSignal analyze(
            MultiTimeframeAnalysis analysis) {

        if (analysis == null) {

            return createWaitSignal(
                    null,
                    0,
                    EntryCondition.NO_CLEAR_SETUP,
                    "Market analysis is not available"
            );
        }

        List<TimeframeAnalysis> timeframes =
                analysis.analyses();

        if (timeframes == null
                || timeframes.isEmpty()) {

            return createWaitSignal(
                    analysis,
                    0,
                    EntryCondition.NO_CLEAR_SETUP,
                    "No timeframe analysis is available"
            );
        }

        /*
         * =====================================================
         * BASE SCORE
         * =====================================================
         */
        int score =
                calculateBaseScore(analysis);

        /*
         * =====================================================
         * MOMENTUM
         * =====================================================
         */
        score += calculateMomentumAdjustment(
                timeframes
        );

        /*
         * =====================================================
         * CANDLE PATTERN
         * =====================================================
         */
        score += calculateCandlePatternAdjustment(
                timeframes
        );

        /*
         * =====================================================
         * SUPPORT / RESISTANCE
         * =====================================================
         */
        score += calculateSupportResistanceAdjustment(
                timeframes
        );

        /*
         * =====================================================
         * VOLUME
         * =====================================================
         */
        score += calculateVolumeAdjustment(
                timeframes
        );

        /*
         * =====================================================
         * LIMIT SCORE
         * =====================================================
         */
        score = Math.max(
                0,
                Math.min(
                        100,
                        score
                )
        );

        /*
         * =====================================================
         * DIRECTION
         * =====================================================
         */
        SignalDirection direction =
                determineDirection(
                        analysis,
                        score
                );

        /*
         * =====================================================
         * ENTRY CONDITION
         * =====================================================
         */
        EntryCondition entryCondition =
                determineEntryCondition(
                        analysis,
                        direction,
                        score
                );

        /*
         * =====================================================
         * REASON
         * =====================================================
         */
        String reason =
                buildReason(
                        analysis,
                        direction,
                        entryCondition
                );

        return new MarketSignal(
                analysis.instrumentKey(),
                analysis.timestamp(),
                direction,
                entryCondition,
                score,
                reason
        );
    }

    /*
     * =========================================================
     * BASE SCORE
     * =========================================================
     */
    private int calculateBaseScore(
            MultiTimeframeAnalysis analysis) {

        TrendAlignment alignment =
                analysis.alignment();

        if (alignment == null) {
            return 50;
        }

        return switch (alignment) {

            case STRONGLY_BULLISH ->
                    80;

            case BULLISH ->
                    65;

            case MIXED_BULLISH ->
                    58;

            case NEUTRAL ->
                    50;

            case MIXED_BEARISH ->
                    42;

            case BEARISH ->
                    35;

            case STRONGLY_BEARISH ->
                    20;

            case INSUFFICIENT_DATA ->
                    50;
        };
    }

    /*
     * =========================================================
     * MOMENTUM ADJUSTMENT
     * =========================================================
     */
    private int calculateMomentumAdjustment(
            List<TimeframeAnalysis> timeframes) {

        int adjustment = 0;

        for (TimeframeAnalysis timeframe :
                timeframes) {

            if (timeframe == null
                    || timeframe.marketState() == null) {

                continue;
            }

            MomentumCondition condition =
                    timeframe.marketState()
                            .momentumCondition();

            if (condition == null) {
                continue;
            }

            switch (condition) {

                case STRONG ->
                        adjustment += 3;

                case NORMAL ->
                        adjustment += 1;

                case WEAK ->
                        adjustment -= 2;

                case OVERBOUGHT ->
                        adjustment -= 3;

                case EXTREME_OVERBOUGHT ->
                        adjustment -= 8;

                case OVERSOLD ->
                        adjustment -= 2;

                case EXTREME_OVERSOLD ->
                        adjustment -= 4;

                case UNKNOWN -> {
                    // No adjustment.
                }
            }
        }

        return adjustment;
    }

    /*
     * =========================================================
     * CANDLE PATTERN ADJUSTMENT
     * =========================================================
     */
    private int calculateCandlePatternAdjustment(
            List<TimeframeAnalysis> timeframes) {

        int adjustment = 0;

        for (TimeframeAnalysis timeframe :
                timeframes) {

            if (timeframe == null
                    || timeframe.candlePattern() == null) {

                continue;
            }

            adjustment +=
                    timeframe.candlePattern().score();
        }

        return adjustment;
    }

    /*
     * =========================================================
     * SUPPORT / RESISTANCE ADJUSTMENT
     * =========================================================
     */
    private int calculateSupportResistanceAdjustment(
            List<TimeframeAnalysis> timeframes) {

        int adjustment = 0;

        for (TimeframeAnalysis timeframe :
                timeframes) {

            if (timeframe == null
                    || timeframe.analysis() == null) {

                continue;
            }

            SupportResistanceAnalysis sr =
                    supportResistanceCalculator.calculate(
                            timeframe.analysis()
                    );

            if (sr == null
                    || sr.position() == null) {

                continue;
            }

            switch (sr.position()) {

                /*
                 * Bullish market:
                 * price near support can provide a better
                 * long-entry location.
                 */
                case NEAR_SUPPORT ->
                        adjustment += 3;

                /*
                 * Price near resistance makes an immediate
                 * bullish entry less attractive.
                 */
                case NEAR_RESISTANCE ->
                        adjustment -= 2;

                /*
                 * Price has moved above resistance.
                 * Potential breakout.
                 */
                case ABOVE_RESISTANCE ->
                        adjustment += 3;

                /*
                 * Price has moved below support.
                 * Potential breakdown.
                 */
                case BELOW_SUPPORT ->
                        adjustment -= 3;

                case BETWEEN_LEVELS,
                     NO_LEVEL_DATA -> {
                    // No adjustment.
                }
            }
        }

        return adjustment;
    }

    /*
     * =========================================================
     * VOLUME ADJUSTMENT
     * =========================================================
     */
    private int calculateVolumeAdjustment(
            List<TimeframeAnalysis> timeframes) {

        int adjustment = 0;

        for (TimeframeAnalysis timeframe :
                timeframes) {

            if (timeframe == null
                    || timeframe.volumeAnalysis() == null) {

                continue;
            }

            VolumeAnalysis volume =
                    timeframe.volumeAnalysis();

            if (volume.condition() == null) {
                continue;
            }

            switch (volume.condition()) {

                case VERY_HIGH ->
                        adjustment += 4;

                case HIGH ->
                        adjustment += 2;

                case NORMAL ->
                        adjustment += 1;

                case LOW ->
                        adjustment -= 1;

                case VERY_LOW ->
                        adjustment -= 2;

                case UNKNOWN -> {
                    // No adjustment.
                }
            }
        }

        return adjustment;
    }

    /*
     * =========================================================
     * MARKET DIRECTION
     * =========================================================
     */
    private SignalDirection determineDirection(
            MultiTimeframeAnalysis analysis,
            int score) {

        TrendAlignment alignment =
                analysis.alignment();

        if (alignment ==
                TrendAlignment.STRONGLY_BULLISH) {

            return score >= 65
                    ? SignalDirection.BUY
                    : SignalDirection.WAIT;
        }

        if (alignment ==
                TrendAlignment.BULLISH) {

            return score >= 70
                    ? SignalDirection.BUY
                    : SignalDirection.WAIT;
        }

        if (alignment ==
                TrendAlignment.MIXED_BULLISH) {

            return score >= 75
                    ? SignalDirection.BUY
                    : SignalDirection.WAIT;
        }

        if (alignment ==
                TrendAlignment.STRONGLY_BEARISH) {

            return score <= 35
                    ? SignalDirection.SELL
                    : SignalDirection.WAIT;
        }

        if (alignment ==
                TrendAlignment.BEARISH) {

            return score <= 30
                    ? SignalDirection.SELL
                    : SignalDirection.WAIT;
        }

        if (alignment ==
                TrendAlignment.MIXED_BEARISH) {

            return score <= 25
                    ? SignalDirection.SELL
                    : SignalDirection.WAIT;
        }

        return SignalDirection.WAIT;
    }

    /*
     * =========================================================
     * ENTRY CONDITION
     * =========================================================
     */
    private EntryCondition determineEntryCondition(
            MultiTimeframeAnalysis analysis,
            SignalDirection direction,
            int score) {

        TrendAlignment alignment =
                analysis.alignment();

        boolean extremeOverbought =
                hasExtremeOverbought(
                        analysis.analyses()
                );

        boolean extremeOversold =
                hasExtremeOversold(
                        analysis.analyses()
                );

        boolean nearResistance =
                hasPosition(
                        analysis.analyses(),
                        PriceLevelPosition.NEAR_RESISTANCE
                );

        boolean nearSupport =
                hasPosition(
                        analysis.analyses(),
                        PriceLevelPosition.NEAR_SUPPORT
                );

        boolean bullishBreakout =
                hasPriceAction(
                        analysis.analyses(),
                        PriceActionSignal.BULLISH_BREAKOUT
                );

        boolean bearishBreakdown =
                hasPriceAction(
                        analysis.analyses(),
                        PriceActionSignal.BEARISH_BREAKDOWN
                );

        /*
         * =====================================================
         * BULLISH
         * =====================================================
         */
        if (isBullishAlignment(alignment)) {

            /*
             * Extreme overbought takes highest priority.
             */
            if (extremeOverbought) {

                return EntryCondition.WAIT_FOR_PULLBACK;
            }

            /*
             * Actual confirmed breakout.
             */
            if (bullishBreakout
                    && direction == SignalDirection.BUY) {

                return EntryCondition.GOOD_TO_BUY;
            }

            /*
             * Price near resistance but no breakout yet.
             */
            if (nearResistance
                    && direction == SignalDirection.BUY) {

                return EntryCondition.WAIT_FOR_BREAKOUT;
            }

            /*
             * Price near support in bullish trend.
             */
            if (nearSupport
                    && direction == SignalDirection.BUY
                    && score >= 70) {

                return EntryCondition.GOOD_TO_BUY;
            }

            if (direction == SignalDirection.BUY
                    && score >= 75) {

                return EntryCondition.GOOD_TO_BUY;
            }

            return EntryCondition.WAIT_FOR_BREAKOUT;
        }

        /*
         * =====================================================
         * BEARISH
         * =====================================================
         */
        if (isBearishAlignment(alignment)) {

            /*
             * Extreme oversold.
             */
            if (extremeOversold) {

                return EntryCondition.WAIT_FOR_PULLBACK;
            }

            /*
             * Actual confirmed breakdown.
             */
            if (bearishBreakdown
                    && direction == SignalDirection.SELL) {

                return EntryCondition.GOOD_TO_SELL;
            }

            /*
             * Price near support.
             */
            if (nearSupport
                    && direction == SignalDirection.SELL) {

                return EntryCondition.WAIT_FOR_BREAKOUT;
            }

            /*
             * Price near resistance in bearish trend.
             */
            if (nearResistance
                    && direction == SignalDirection.SELL
                    && score <= 30) {

                return EntryCondition.GOOD_TO_SELL;
            }

            if (direction == SignalDirection.SELL
                    && score <= 25) {

                return EntryCondition.GOOD_TO_SELL;
            }

            return EntryCondition.WAIT_FOR_BREAKOUT;
        }

        /*
         * =====================================================
         * NEUTRAL
         * =====================================================
         */
        return EntryCondition.NO_CLEAR_SETUP;
    }

    /*
     * =========================================================
     * CHECK SUPPORT / RESISTANCE POSITION
     * =========================================================
     */
    private boolean hasPosition(
            List<TimeframeAnalysis> timeframes,
            PriceLevelPosition target) {

        if (timeframes == null
                || target == null) {

            return false;
        }

        for (TimeframeAnalysis timeframe :
                timeframes) {

            if (timeframe == null
                    || timeframe.analysis() == null) {

                continue;
            }

            SupportResistanceAnalysis sr =
                    supportResistanceCalculator.calculate(
                            timeframe.analysis()
                    );

            if (sr != null
                    && sr.position() == target) {

                return true;
            }
        }

        return false;
    }

    /*
     * =========================================================
     * CHECK PRICE ACTION
     * =========================================================
     */
    private boolean hasPriceAction(
            List<TimeframeAnalysis> timeframes,
            PriceActionSignal target) {

        if (timeframes == null
                || target == null) {

            return false;
        }

        for (TimeframeAnalysis timeframe :
                timeframes) {

            if (timeframe == null
                    || timeframe.priceAction() == null) {

                continue;
            }

            PriceActionAnalysis priceAction =
                    timeframe.priceAction();

            if (priceAction.confirmed()
                    && priceAction.signal() == target) {

                return true;
            }
        }

        return false;
    }

    /*
     * =========================================================
     * BULLISH ALIGNMENT
     * =========================================================
     */
    private boolean isBullishAlignment(
            TrendAlignment alignment) {

        return alignment ==
                TrendAlignment.STRONGLY_BULLISH

                || alignment ==
                TrendAlignment.BULLISH

                || alignment ==
                TrendAlignment.MIXED_BULLISH;
    }

    /*
     * =========================================================
     * BEARISH ALIGNMENT
     * =========================================================
     */
    private boolean isBearishAlignment(
            TrendAlignment alignment) {

        return alignment ==
                TrendAlignment.STRONGLY_BEARISH

                || alignment ==
                TrendAlignment.BEARISH

                || alignment ==
                TrendAlignment.MIXED_BEARISH;
    }

    /*
     * =========================================================
     * EXTREME OVERBOUGHT
     * =========================================================
     */
    private boolean hasExtremeOverbought(
            List<TimeframeAnalysis> timeframes) {

        if (timeframes == null) {
            return false;
        }

        for (TimeframeAnalysis timeframe :
                timeframes) {

            if (timeframe == null
                    || timeframe.marketState() == null) {

                continue;
            }

            if (timeframe.marketState()
                    .momentumCondition()
                    == MomentumCondition.EXTREME_OVERBOUGHT) {

                return true;
            }
        }

        return false;
    }

    /*
     * =========================================================
     * EXTREME OVERSOLD
     * =========================================================
     */
    private boolean hasExtremeOversold(
            List<TimeframeAnalysis> timeframes) {

        if (timeframes == null) {
            return false;
        }

        for (TimeframeAnalysis timeframe :
                timeframes) {

            if (timeframe == null
                    || timeframe.marketState() == null) {

                continue;
            }

            if (timeframe.marketState()
                    .momentumCondition()
                    == MomentumCondition.EXTREME_OVERSOLD) {

                return true;
            }
        }

        return false;
    }

    /*
     * =========================================================
     * BUILD REASON
     * =========================================================
     */
    private String buildReason(
            MultiTimeframeAnalysis analysis,
            SignalDirection direction,
            EntryCondition entryCondition) {

        if (entryCondition ==
                EntryCondition.WAIT_FOR_PULLBACK) {

            if (hasExtremeOverbought(
                    analysis.analyses()
            )) {

                return "Bullish trend but momentum is extremely overbought; wait for a pullback";
            }

            if (hasExtremeOversold(
                    analysis.analyses()
            )) {

                return "Bearish trend but momentum is extremely oversold; wait for a pullback";
            }
        }

        if (entryCondition ==
                EntryCondition.WAIT_FOR_BREAKOUT) {

            if (direction ==
                    SignalDirection.BUY) {

                return "Bullish trend but price is near resistance; wait for breakout confirmation";
            }

            if (direction ==
                    SignalDirection.SELL) {

                return "Bearish trend but price is near support; wait for breakdown confirmation";
            }

            return "Directional bias exists but breakout confirmation is required";
        }

        if (entryCondition ==
                EntryCondition.GOOD_TO_BUY) {

            return "Bullish trend with favorable support, momentum, price-action and volume conditions";
        }

        if (entryCondition ==
                EntryCondition.GOOD_TO_SELL) {

            return "Bearish trend with favorable resistance, momentum, price-action and volume conditions";
        }

        if (entryCondition ==
                EntryCondition.NO_CLEAR_SETUP) {

            return "Timeframes are not showing a clear directional trading setup";
        }

        return "Trading conditions are not strong enough for an immediate entry";
    }

    /*
     * =========================================================
     * CREATE WAIT SIGNAL
     * =========================================================
     */
    private MarketSignal createWaitSignal(
            MultiTimeframeAnalysis analysis,
            int score,
            EntryCondition entryCondition,
            String reason) {

        return new MarketSignal(
                analysis == null
                        ? null
                        : analysis.instrumentKey(),

                analysis == null
                        ? null
                        : analysis.timestamp(),

                SignalDirection.WAIT,

                entryCondition,

                score,

                reason
        );
    }
}