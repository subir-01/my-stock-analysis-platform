package com.Dashboard.myTradingPlatform.market.analytics.calculator;

import com.Dashboard.myTradingPlatform.market.model.MarketCandle;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class RsiCalculator {

    private static final int CALCULATION_SCALE = 10;
    private static final int RESULT_SCALE = 4;

    private static final BigDecimal ONE_HUNDRED =
            BigDecimal.valueOf(100);

    private static final BigDecimal ZERO =
            BigDecimal.ZERO;

    public BigDecimal calculate(
            List<MarketCandle> candles,
            int period) {

        /*
         * =====================================================
         * VALIDATION
         * =====================================================
         *
         * RSI requires at least:
         *
         * period + 1 candles
         *
         * because we need 'period' price changes.
         */
        if (candles == null
                || candles.isEmpty()
                || period <= 0
                || candles.size() <= period) {

            return null;
        }

        /*
         * =====================================================
         * INITIAL GAIN / LOSS
         * =====================================================
         *
         * Calculate the first 'period' price changes.
         */
        BigDecimal gainSum =
                ZERO;

        BigDecimal lossSum =
                ZERO;

        for (int i = 1;
             i <= period;
             i++) {

            MarketCandle previous =
                    candles.get(i - 1);

            MarketCandle current =
                    candles.get(i);

            if (previous == null
                    || current == null
                    || previous.close() == null
                    || current.close() == null) {

                return null;
            }

            BigDecimal change =
                    current.close()
                            .subtract(
                                    previous.close()
                            );

            if (change.compareTo(ZERO) > 0) {

                gainSum =
                        gainSum.add(change);

            } else if (change.compareTo(ZERO) < 0) {

                lossSum =
                        lossSum.add(
                                change.abs()
                        );
            }
        }

        /*
         * =====================================================
         * INITIAL AVERAGE GAIN / LOSS
         * =====================================================
         */
        BigDecimal periodValue =
                BigDecimal.valueOf(period);

        BigDecimal averageGain =
                gainSum.divide(
                        periodValue,
                        CALCULATION_SCALE,
                        RoundingMode.HALF_UP
                );

        BigDecimal averageLoss =
                lossSum.divide(
                        periodValue,
                        CALCULATION_SCALE,
                        RoundingMode.HALF_UP
                );

        /*
         * =====================================================
         * WILDER'S RSI SMOOTHING
         * =====================================================
         *
         * Average Gain:
         *
         * ((Previous Average Gain × (period - 1))
         *  + Current Gain) / period
         *
         * Average Loss:
         *
         * ((Previous Average Loss × (period - 1))
         *  + Current Loss) / period
         */
        BigDecimal smoothingFactor =
                BigDecimal.valueOf(
                        period - 1
                );

        for (int i = period + 1;
             i < candles.size();
             i++) {

            MarketCandle previous =
                    candles.get(i - 1);

            MarketCandle current =
                    candles.get(i);

            if (previous == null
                    || current == null
                    || previous.close() == null
                    || current.close() == null) {

                return null;
            }

            BigDecimal change =
                    current.close()
                            .subtract(
                                    previous.close()
                            );

            BigDecimal gain =
                    ZERO;

            BigDecimal loss =
                    ZERO;

            if (change.compareTo(ZERO) > 0) {

                gain = change;

            } else if (change.compareTo(ZERO) < 0) {

                loss = change.abs();
            }

            averageGain =
                    averageGain
                            .multiply(smoothingFactor)
                            .add(gain)
                            .divide(
                                    periodValue,
                                    CALCULATION_SCALE,
                                    RoundingMode.HALF_UP
                            );

            averageLoss =
                    averageLoss
                            .multiply(smoothingFactor)
                            .add(loss)
                            .divide(
                                    periodValue,
                                    CALCULATION_SCALE,
                                    RoundingMode.HALF_UP
                            );
        }

        /*
         * =====================================================
         * EDGE CASE: NO LOSSES
         * =====================================================
         *
         * If average loss is zero, the market has moved
         * upward throughout the calculation period.
         *
         * RSI = 100
         */
        if (averageLoss.compareTo(ZERO) == 0) {

            return ONE_HUNDRED.setScale(
                    RESULT_SCALE,
                    RoundingMode.HALF_UP
            );
        }

        /*
         * =====================================================
         * EDGE CASE: NO GAINS
         * =====================================================
         *
         * If average gain is zero while losses exist:
         *
         * RSI = 0
         */
        if (averageGain.compareTo(ZERO) == 0) {

            return ZERO.setScale(
                    RESULT_SCALE,
                    RoundingMode.HALF_UP
            );
        }

        /*
         * =====================================================
         * RELATIVE STRENGTH
         * =====================================================
         *
         * RS = Average Gain / Average Loss
         */
        BigDecimal relativeStrength =
                averageGain.divide(
                        averageLoss,
                        CALCULATION_SCALE,
                        RoundingMode.HALF_UP
                );

        /*
         * =====================================================
         * RSI
         * =====================================================
         *
         * RSI = 100 - (100 / (1 + RS))
         */
        BigDecimal denominator =
                BigDecimal.ONE.add(
                        relativeStrength
                );

        BigDecimal rsi =
                ONE_HUNDRED.divide(
                        denominator,
                        CALCULATION_SCALE,
                        RoundingMode.HALF_UP
                );

        return ONE_HUNDRED
                .subtract(rsi)
                .setScale(
                        RESULT_SCALE,
                        RoundingMode.HALF_UP
                );
    }
}