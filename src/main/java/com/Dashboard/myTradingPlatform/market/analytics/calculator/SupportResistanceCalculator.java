package com.Dashboard.myTradingPlatform.market.analytics.calculator;

import com.Dashboard.myTradingPlatform.market.analytics.model.MarketAnalysis;
import com.Dashboard.myTradingPlatform.market.analytics.model.PriceLevelPosition;
import com.Dashboard.myTradingPlatform.market.analytics.model.SupportResistanceAnalysis;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class SupportResistanceCalculator {

    /*
     * =========================================================
     * PROXIMITY THRESHOLDS
     * =========================================================
     *
     * If price is within this percentage of a support/resistance
     * level, we consider the price to be "near" that level.
     *
     * Example:
     *
     * Price = 1313
     *
     * 0.5% = approximately 6.56 points
     *
     * Therefore a resistance around 1319 could be considered
     * nearby.
     */
    private static final BigDecimal INTRADAY_THRESHOLD =
            new BigDecimal("0.50");

    private static final BigDecimal DAILY_THRESHOLD =
            new BigDecimal("1.00");

    /*
     * =========================================================
     * MAIN CALCULATION
     * =========================================================
     */
    public SupportResistanceAnalysis calculate(
            MarketAnalysis analysis) {

        if (analysis == null) {

            return null;
        }

        BigDecimal price =
                analysis.price();

        if (price == null
                || price.signum() <= 0) {

            return new SupportResistanceAnalysis(
                    analysis.timeframe(),
                    price,
                    analysis.support(),
                    analysis.resistance(),
                    PriceLevelPosition.NO_LEVEL_DATA,
                    null,
                    null
            );
        }

        BigDecimal support =
                analysis.support();

        BigDecimal resistance =
                analysis.resistance();

        BigDecimal distanceFromSupport =
                calculateDistancePercent(
                        price,
                        support
                );

        BigDecimal distanceFromResistance =
                calculateDistancePercent(
                        price,
                        resistance
                );

        PriceLevelPosition position =
                determinePosition(
                        analysis.timeframe(),
                        price,
                        support,
                        resistance,
                        distanceFromSupport,
                        distanceFromResistance
                );

        return new SupportResistanceAnalysis(
                analysis.timeframe(),
                price,
                support,
                resistance,
                position,
                distanceFromSupport,
                distanceFromResistance
        );
    }

    /*
     * =========================================================
     * DETERMINE POSITION
     * =========================================================
     */
    private PriceLevelPosition determinePosition(
            String timeframe,
            BigDecimal price,
            BigDecimal support,
            BigDecimal resistance,
            BigDecimal distanceFromSupport,
            BigDecimal distanceFromResistance) {

        if (support == null
                && resistance == null) {

            return PriceLevelPosition.NO_LEVEL_DATA;
        }

        /*
         * =====================================================
         * ABOVE RESISTANCE
         * =====================================================
         */
        if (resistance != null
                && price.compareTo(resistance) > 0) {

            return PriceLevelPosition.ABOVE_RESISTANCE;
        }

        /*
         * =====================================================
         * BELOW SUPPORT
         * =====================================================
         */
        if (support != null
                && price.compareTo(support) < 0) {

            return PriceLevelPosition.BELOW_SUPPORT;
        }

        BigDecimal threshold =
                getThreshold(timeframe);

        /*
         * =====================================================
         * NEAR SUPPORT
         * =====================================================
         */
        if (distanceFromSupport != null
                && distanceFromSupport.compareTo(
                threshold
        ) <= 0) {

            return PriceLevelPosition.NEAR_SUPPORT;
        }

        /*
         * =====================================================
         * NEAR RESISTANCE
         * =====================================================
         */
        if (distanceFromResistance != null
                && distanceFromResistance.compareTo(
                threshold
        ) <= 0) {

            return PriceLevelPosition.NEAR_RESISTANCE;
        }

        /*
         * =====================================================
         * BETWEEN SUPPORT AND RESISTANCE
         * =====================================================
         */
        if (support != null
                && resistance != null
                && price.compareTo(support) >= 0
                && price.compareTo(resistance) <= 0) {

            return PriceLevelPosition.BETWEEN_LEVELS;
        }

        return PriceLevelPosition.NO_LEVEL_DATA;
    }

    /*
     * =========================================================
     * DISTANCE %
     * =========================================================
     */
    private BigDecimal calculateDistancePercent(
            BigDecimal price,
            BigDecimal level) {

        if (level == null
                || level.signum() <= 0) {

            return null;
        }

        return price
                .subtract(level)
                .abs()
                .divide(
                        level,
                        8,
                        RoundingMode.HALF_UP
                )
                .multiply(
                        new BigDecimal("100")
                )
                .setScale(
                        4,
                        RoundingMode.HALF_UP
                );
    }

    /*
     * =========================================================
     * TIMEFRAME THRESHOLD
     * =========================================================
     */
    private BigDecimal getThreshold(
            String timeframe) {

        if ("1d".equalsIgnoreCase(timeframe)) {

            return DAILY_THRESHOLD;
        }

        return INTRADAY_THRESHOLD;
    }
}