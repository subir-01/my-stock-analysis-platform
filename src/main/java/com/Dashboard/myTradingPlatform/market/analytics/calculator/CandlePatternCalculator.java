package com.Dashboard.myTradingPlatform.market.analytics.calculator;

import com.Dashboard.myTradingPlatform.market.analytics.model.CandlePatternAnalysis;
import com.Dashboard.myTradingPlatform.market.analytics.model.PatternDirection;
import com.Dashboard.myTradingPlatform.market.analytics.model.PatternStrength;
import com.Dashboard.myTradingPlatform.market.model.MarketCandle;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Component
public class CandlePatternCalculator {

    private static final BigDecimal DOJI_THRESHOLD =
            new BigDecimal("0.10");

    /*
     * =========================================================
     * MAIN CALCULATION
     * =========================================================
     */
    public CandlePatternAnalysis calculate(
            List<MarketCandle> candles) {

        if (candles == null
                || candles.isEmpty()) {

            return null;
        }

        /*
         * IMPORTANT:
         *
         * Only analyse the latest candle.
         *
         * Previously we searched backwards for a usable candle.
         * That could result in a pattern from several days/weeks
         * ago being treated as the current pattern.
         */
        MarketCandle latest =
                candles.get(candles.size() - 1);

        /*
         * If the latest candle has no meaningful range,
         * we cannot reliably identify a candle pattern.
         *
         * This commonly happens with a currently forming
         * candle when:
         *
         * open = high = low = close
         */
        if (!isUsable(latest)) {

            return null;
        }

        /*
         * =====================================================
         * THREE-CANDLE PATTERNS
         * =====================================================
         *
         * Need at least three candles.
         */
        if (candles.size() >= 3) {

            MarketCandle first =
                    candles.get(candles.size() - 3);

            MarketCandle second =
                    candles.get(candles.size() - 2);

            MarketCandle third =
                    candles.get(candles.size() - 1);

            /*
             * Morning Star
             */
            if (isUsable(first)
                    && isUsable(second)
                    && isUsable(third)
                    && isMorningStar(
                    first,
                    second,
                    third
            )) {

                return createAnalysis(
                        "MORNING_STAR",
                        PatternDirection.BULLISH,
                        PatternStrength.STRONG,
                        3,
                        third
                );
            }

            /*
             * Evening Star
             */
            if (isUsable(first)
                    && isUsable(second)
                    && isUsable(third)
                    && isEveningStar(
                    first,
                    second,
                    third
            )) {

                return createAnalysis(
                        "EVENING_STAR",
                        PatternDirection.BEARISH,
                        PatternStrength.STRONG,
                        -3,
                        third
                );
            }
        }

        /*
         * =====================================================
         * TWO-CANDLE PATTERNS
         * =====================================================
         */
        if (candles.size() >= 2) {

            MarketCandle previous =
                    candles.get(candles.size() - 2);

            MarketCandle current =
                    candles.get(candles.size() - 1);

            if (isUsable(previous)
                    && isUsable(current)) {

                /*
                 * Bullish Engulfing
                 */
                if (isBullishEngulfing(
                        previous,
                        current
                )) {

                    return createAnalysis(
                            "BULLISH_ENGULFING",
                            PatternDirection.BULLISH,
                            PatternStrength.STRONG,
                            3,
                            current
                    );
                }

                /*
                 * Bearish Engulfing
                 */
                if (isBearishEngulfing(
                        previous,
                        current
                )) {

                    return createAnalysis(
                            "BEARISH_ENGULFING",
                            PatternDirection.BEARISH,
                            PatternStrength.STRONG,
                            -3,
                            current
                    );
                }
            }
        }

        /*
         * =====================================================
         * SINGLE-CANDLE PATTERNS
         * =====================================================
         */

        /*
         * Doji
         */
        if (isDoji(latest)) {

            return createAnalysis(
                    "DOJI",
                    PatternDirection.NEUTRAL,
                    PatternStrength.MEDIUM,
                    0,
                    latest
            );
        }

        /*
         * Hammer
         */
        if (isHammer(latest)) {

            return createAnalysis(
                    "HAMMER",
                    PatternDirection.BULLISH,
                    PatternStrength.MEDIUM,
                    2,
                    latest
            );
        }

        /*
         * Shooting Star
         */
        if (isShootingStar(latest)) {

            return createAnalysis(
                    "SHOOTING_STAR",
                    PatternDirection.BEARISH,
                    PatternStrength.MEDIUM,
                    -2,
                    latest
            );
        }

        /*
         * Spinning Top
         */
        if (isSpinningTop(latest)) {

            return createAnalysis(
                    "SPINNING_TOP",
                    PatternDirection.NEUTRAL,
                    PatternStrength.WEAK,
                    0,
                    latest
            );
        }

        /*
         * No recognised pattern.
         */
        return createAnalysis(
                "NONE",
                PatternDirection.NEUTRAL,
                PatternStrength.WEAK,
                0,
                latest
        );
    }

    /*
     * =========================================================
     * USABLE CANDLE
     * =========================================================
     */
    private boolean isUsable(
            MarketCandle candle) {

        if (candle == null) {
            return false;
        }

        if (candle.open() == null
                || candle.high() == null
                || candle.low() == null
                || candle.close() == null) {

            return false;
        }

        return candle.high()
                .compareTo(candle.low()) > 0;
    }

    /*
     * =========================================================
     * BULLISH ENGULFING
     * =========================================================
     */
    private boolean isBullishEngulfing(
            MarketCandle previous,
            MarketCandle current) {

        boolean previousBearish =
                previous.close()
                        .compareTo(previous.open()) < 0;

        boolean currentBullish =
                current.close()
                        .compareTo(current.open()) > 0;

        boolean opensBelowPreviousClose =
                current.open()
                        .compareTo(previous.close()) <= 0;

        boolean closesAbovePreviousOpen =
                current.close()
                        .compareTo(previous.open()) >= 0;

        return previousBearish
                && currentBullish
                && opensBelowPreviousClose
                && closesAbovePreviousOpen;
    }

    /*
     * =========================================================
     * BEARISH ENGULFING
     * =========================================================
     */
    private boolean isBearishEngulfing(
            MarketCandle previous,
            MarketCandle current) {

        boolean previousBullish =
                previous.close()
                        .compareTo(previous.open()) > 0;

        boolean currentBearish =
                current.close()
                        .compareTo(current.open()) < 0;

        boolean opensAbovePreviousClose =
                current.open()
                        .compareTo(previous.close()) >= 0;

        boolean closesBelowPreviousOpen =
                current.close()
                        .compareTo(previous.open()) <= 0;

        return previousBullish
                && currentBearish
                && opensAbovePreviousClose
                && closesBelowPreviousOpen;
    }

    /*
     * =========================================================
     * MORNING STAR
     * =========================================================
     */
    private boolean isMorningStar(
            MarketCandle first,
            MarketCandle second,
            MarketCandle third) {

        boolean firstBearish =
                first.close()
                        .compareTo(first.open()) < 0;

        boolean thirdBullish =
                third.close()
                        .compareTo(third.open()) > 0;

        BigDecimal firstBody =
                bodySize(first);

        BigDecimal secondBody =
                bodySize(second);

        BigDecimal firstRange =
                range(first);

        boolean secondSmall =
                secondBody.compareTo(
                        firstRange.multiply(
                                new BigDecimal("0.40")
                        )
                ) <= 0;

        BigDecimal firstMidpoint =
                first.open()
                        .add(first.close())
                        .divide(
                                new BigDecimal("2"),
                                8,
                                RoundingMode.HALF_UP
                        );

        boolean thirdClosesAboveMidpoint =
                third.close()
                        .compareTo(firstMidpoint) > 0;

        return firstBearish
                && secondSmall
                && thirdBullish
                && thirdClosesAboveMidpoint;
    }

    /*
     * =========================================================
     * EVENING STAR
     * =========================================================
     */
    private boolean isEveningStar(
            MarketCandle first,
            MarketCandle second,
            MarketCandle third) {

        boolean firstBullish =
                first.close()
                        .compareTo(first.open()) > 0;

        boolean thirdBearish =
                third.close()
                        .compareTo(third.open()) < 0;

        BigDecimal secondBody =
                bodySize(second);

        BigDecimal firstRange =
                range(first);

        boolean secondSmall =
                secondBody.compareTo(
                        firstRange.multiply(
                                new BigDecimal("0.40")
                        )
                ) <= 0;

        BigDecimal firstMidpoint =
                first.open()
                        .add(first.close())
                        .divide(
                                new BigDecimal("2"),
                                8,
                                RoundingMode.HALF_UP
                        );

        boolean thirdClosesBelowMidpoint =
                third.close()
                        .compareTo(firstMidpoint) < 0;

        return firstBullish
                && secondSmall
                && thirdBearish
                && thirdClosesBelowMidpoint;
    }

    /*
     * =========================================================
     * DOJI
     * =========================================================
     */
    private boolean isDoji(
            MarketCandle candle) {

        BigDecimal body =
                bodySize(candle);

        BigDecimal candleRange =
                range(candle);

        if (candleRange.signum() == 0) {
            return false;
        }

        BigDecimal bodyRatio =
                body.divide(
                        candleRange,
                        8,
                        RoundingMode.HALF_UP
                );

        return bodyRatio.compareTo(
                DOJI_THRESHOLD
        ) <= 0;
    }

    /*
     * =========================================================
     * HAMMER
     * =========================================================
     */
    private boolean isHammer(
            MarketCandle candle) {

        BigDecimal body =
                bodySize(candle);

        BigDecimal upperWick =
                upperWick(candle);

        BigDecimal lowerWick =
                lowerWick(candle);

        if (body.signum() == 0) {
            return false;
        }

        return lowerWick.compareTo(
                body.multiply(
                        new BigDecimal("2")
                )
        ) >= 0
                && upperWick.compareTo(body) <= 0;
    }

    /*
     * =========================================================
     * SHOOTING STAR
     * =========================================================
     */
    private boolean isShootingStar(
            MarketCandle candle) {

        BigDecimal body =
                bodySize(candle);

        BigDecimal upperWick =
                upperWick(candle);

        BigDecimal lowerWick =
                lowerWick(candle);

        if (body.signum() == 0) {
            return false;
        }

        return upperWick.compareTo(
                body.multiply(
                        new BigDecimal("2")
                )
        ) >= 0
                && lowerWick.compareTo(body) <= 0;
    }

    /*
     * =========================================================
     * SPINNING TOP
     * =========================================================
     */
    private boolean isSpinningTop(
            MarketCandle candle) {

        BigDecimal body =
                bodySize(candle);

        BigDecimal candleRange =
                range(candle);

        if (candleRange.signum() == 0) {
            return false;
        }

        BigDecimal bodyRatio =
                body.divide(
                        candleRange,
                        8,
                        RoundingMode.HALF_UP
                );

        return bodyRatio.compareTo(
                new BigDecimal("0.30")
        ) <= 0
                && !isDoji(candle);
    }

    /*
     * =========================================================
     * BODY SIZE
     * =========================================================
     */
    private BigDecimal bodySize(
            MarketCandle candle) {

        return candle.close()
                .subtract(candle.open())
                .abs();
    }

    /*
     * =========================================================
     * RANGE
     * =========================================================
     */
    private BigDecimal range(
            MarketCandle candle) {

        return candle.high()
                .subtract(candle.low())
                .abs();
    }

    /*
     * =========================================================
     * UPPER WICK
     * =========================================================
     */
    private BigDecimal upperWick(
            MarketCandle candle) {

        BigDecimal maxOpenClose =
                candle.open().max(
                        candle.close()
                );

        return candle.high()
                .subtract(maxOpenClose);
    }

    /*
     * =========================================================
     * LOWER WICK
     * =========================================================
     */
    private BigDecimal lowerWick(
            MarketCandle candle) {

        BigDecimal minOpenClose =
                candle.open().min(
                        candle.close()
                );

        return minOpenClose
                .subtract(candle.low());
    }

    /*
     * =========================================================
     * CREATE RESULT
     * =========================================================
     */
    private CandlePatternAnalysis createAnalysis(
            String pattern,
            PatternDirection direction,
            PatternStrength strength,
            int score,
            MarketCandle candle) {

        Instant timestamp =
                candle.timestamp();

        return new CandlePatternAnalysis(
                pattern,
                direction,
                strength,
                score,
                timestamp,
                candle.open(),
                candle.high(),
                candle.low(),
                candle.close()
        );
    }
}