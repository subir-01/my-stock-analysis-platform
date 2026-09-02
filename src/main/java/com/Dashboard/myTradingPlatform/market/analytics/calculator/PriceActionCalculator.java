package com.Dashboard.myTradingPlatform.market.analytics.calculator;

import com.Dashboard.myTradingPlatform.market.analytics.model.MarketAnalysis;
import com.Dashboard.myTradingPlatform.market.analytics.model.PriceActionAnalysis;
import com.Dashboard.myTradingPlatform.market.analytics.model.PriceActionSignal;
import com.Dashboard.myTradingPlatform.market.model.MarketCandle;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Component
public class PriceActionCalculator {

    /*
     * =========================================================
     * BREAKOUT BUFFER
     * =========================================================
     *
     * We don't want a tiny price movement above resistance
     * to immediately be classified as a breakout.
     *
     * Example:
     *
     * Resistance = 1314
     *
     * 0.10% buffer ≈ 1.31 points
     *
     * Breakout confirmation level ≈ 1315.31
     */
    private static final BigDecimal BREAKOUT_BUFFER_PERCENT =
            new BigDecimal("0.10");

    /*
     * =========================================================
     * BOUNCE BUFFER
     * =========================================================
     *
     * Used to determine whether price has interacted with
     * support/resistance.
     */
    private static final BigDecimal BOUNCE_BUFFER_PERCENT =
            new BigDecimal("0.30");

    /*
     * =========================================================
     * MAIN CALCULATION
     * =========================================================
     */
    public PriceActionAnalysis calculate(
            MarketAnalysis analysis,
            List<MarketCandle> candles) {

        if (analysis == null) {
            return null;
        }

        if (candles == null
                || candles.size() < 2) {

            return createNoneResult(
                    analysis,
                    null
            );
        }

        MarketCandle previous =
                candles.get(
                        candles.size() - 2
                );

        MarketCandle current =
                candles.get(
                        candles.size() - 1
                );

        if (!isUsable(previous)
                || !isUsable(current)) {

            return createNoneResult(
                    analysis,
                    current
            );
        }

        BigDecimal price =
                current.close();

        BigDecimal previousClose =
                previous.close();

        BigDecimal support =
                analysis.support();

        BigDecimal resistance =
                analysis.resistance();

        /*
         * =====================================================
         * BULLISH BREAKOUT
         * =====================================================
         */
        if (isBullishBreakout(
                previousClose,
                price,
                resistance
        )) {

            return new PriceActionAnalysis(
                    analysis.timeframe(),
                    current.timestamp(),
                    PriceActionSignal.BULLISH_BREAKOUT,
                    price,
                    previousClose,
                    support,
                    resistance,
                    true
            );
        }

        /*
         * =====================================================
         * BEARISH BREAKDOWN
         * =====================================================
         */
        if (isBearishBreakdown(
                previousClose,
                price,
                support
        )) {

            return new PriceActionAnalysis(
                    analysis.timeframe(),
                    current.timestamp(),
                    PriceActionSignal.BEARISH_BREAKDOWN,
                    price,
                    previousClose,
                    support,
                    resistance,
                    true
            );
        }

        /*
         * =====================================================
         * BULLISH BOUNCE
         * =====================================================
         */
        if (isBullishBounce(
                previous,
                current,
                support
        )) {

            return new PriceActionAnalysis(
                    analysis.timeframe(),
                    current.timestamp(),
                    PriceActionSignal.BULLISH_BOUNCE,
                    price,
                    previousClose,
                    support,
                    resistance,
                    true
            );
        }

        /*
         * =====================================================
         * BEARISH REJECTION
         * =====================================================
         */
        if (isBearishRejection(
                previous,
                current,
                resistance
        )) {

            return new PriceActionAnalysis(
                    analysis.timeframe(),
                    current.timestamp(),
                    PriceActionSignal.BEARISH_REJECTION,
                    price,
                    previousClose,
                    support,
                    resistance,
                    true
            );
        }

        return new PriceActionAnalysis(
                analysis.timeframe(),
                current.timestamp(),
                PriceActionSignal.NONE,
                price,
                previousClose,
                support,
                resistance,
                false
        );
    }

    /*
     * =========================================================
     * BULLISH BREAKOUT
     * =========================================================
     *
     * Previous candle must be at or below resistance.
     *
     * Current candle must close above resistance + buffer.
     */
    private boolean isBullishBreakout(
            BigDecimal previousClose,
            BigDecimal currentClose,
            BigDecimal resistance) {

        if (resistance == null) {
            return false;
        }

        BigDecimal buffer =
                percentageOf(
                        resistance,
                        BREAKOUT_BUFFER_PERCENT
                );

        BigDecimal confirmationLevel =
                resistance.add(buffer);

        return previousClose.compareTo(
                resistance
        ) <= 0

                && currentClose.compareTo(
                confirmationLevel
        ) >= 0;
    }

    /*
     * =========================================================
     * BEARISH BREAKDOWN
     * =========================================================
     */
    private boolean isBearishBreakdown(
            BigDecimal previousClose,
            BigDecimal currentClose,
            BigDecimal support) {

        if (support == null) {
            return false;
        }

        BigDecimal buffer =
                percentageOf(
                        support,
                        BREAKOUT_BUFFER_PERCENT
                );

        BigDecimal confirmationLevel =
                support.subtract(buffer);

        return previousClose.compareTo(
                support
        ) >= 0

                && currentClose.compareTo(
                confirmationLevel
        ) <= 0;
    }

    /*
     * =========================================================
     * BULLISH BOUNCE
     * =========================================================
     *
     * Previous candle touches/approaches support.
     *
     * Current candle closes bullish.
     */
    private boolean isBullishBounce(
            MarketCandle previous,
            MarketCandle current,
            BigDecimal support) {

        if (support == null) {
            return false;
        }

        BigDecimal buffer =
                percentageOf(
                        support,
                        BOUNCE_BUFFER_PERCENT
                );

        BigDecimal lowerBoundary =
                support.subtract(buffer);

        BigDecimal upperBoundary =
                support.add(buffer);

        boolean previousTouchedSupport =
                previous.low().compareTo(
                        upperBoundary
                ) <= 0

                        && previous.high().compareTo(
                        lowerBoundary
                ) >= 0;

        boolean currentBullish =
                current.close().compareTo(
                        current.open()
                ) > 0;

        boolean currentClosesAbovePreviousClose =
                current.close().compareTo(
                        previous.close()
                ) > 0;

        return previousTouchedSupport
                && currentBullish
                && currentClosesAbovePreviousClose;
    }

    /*
     * =========================================================
     * BEARISH REJECTION
     * =========================================================
     *
     * Previous candle interacts with resistance.
     *
     * Current candle closes bearish.
     */
    private boolean isBearishRejection(
            MarketCandle previous,
            MarketCandle current,
            BigDecimal resistance) {

        if (resistance == null) {
            return false;
        }

        BigDecimal buffer =
                percentageOf(
                        resistance,
                        BOUNCE_BUFFER_PERCENT
                );

        BigDecimal lowerBoundary =
                resistance.subtract(buffer);

        BigDecimal upperBoundary =
                resistance.add(buffer);

        boolean previousTouchedResistance =
                previous.high().compareTo(
                        lowerBoundary
                ) >= 0

                        && previous.low().compareTo(
                        upperBoundary
                ) <= 0;

        boolean currentBearish =
                current.close().compareTo(
                        current.open()
                ) < 0;

        boolean currentClosesBelowPreviousClose =
                current.close().compareTo(
                        previous.close()
                ) < 0;

        return previousTouchedResistance
                && currentBearish
                && currentClosesBelowPreviousClose;
    }

    /*
     * =========================================================
     * PERCENTAGE
     * =========================================================
     */
    private BigDecimal percentageOf(
            BigDecimal value,
            BigDecimal percentage) {

        return value
                .multiply(percentage)
                .divide(
                        new BigDecimal("100"),
                        8,
                        RoundingMode.HALF_UP
                );
    }

    /*
     * =========================================================
     * VALID CANDLE
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

        return candle.high().compareTo(
                candle.low()
        ) > 0;
    }

    /*
     * =========================================================
     * NONE RESULT
     * =========================================================
     */
    private PriceActionAnalysis createNoneResult(
            MarketAnalysis analysis,
            MarketCandle candle) {

        Instant timestamp =
                candle == null
                        ? analysis.timestamp()
                        : candle.timestamp();

        BigDecimal price =
                candle == null
                        ? analysis.price()
                        : candle.close();

        return new PriceActionAnalysis(
                analysis.timeframe(),
                timestamp,
                PriceActionSignal.NONE,
                price,
                null,
                analysis.support(),
                analysis.resistance(),
                false
        );
    }
}