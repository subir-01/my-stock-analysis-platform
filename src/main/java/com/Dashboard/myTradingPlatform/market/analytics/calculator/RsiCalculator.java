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

    public BigDecimal calculate(
            List<MarketCandle> candles,
            int period) {

        if (candles == null
                || candles.size() <= period
                || period <= 0) {

            return null;
        }

        /*
         * ---------------------------------------------------------
         * Step 1: Initial average gain/loss
         * ---------------------------------------------------------
         *
         * We need 'period' price changes.
         *
         * For RSI 14:
         *
         * candle 0 -> candle 1
         * candle 1 -> candle 2
         * ...
         * candle 13 -> candle 14
         */
        BigDecimal gainSum =
                BigDecimal.ZERO;

        BigDecimal lossSum =
                BigDecimal.ZERO;

        for (int i = 1; i <= period; i++) {

            BigDecimal currentClose =
                    getClose(candles.get(i));

            BigDecimal previousClose =
                    getClose(candles.get(i - 1));

            if (currentClose == null
                    || previousClose == null) {

                return null;
            }

            BigDecimal change =
                    currentClose.subtract(
                            previousClose
                    );

            if (change.compareTo(BigDecimal.ZERO) > 0) {

                gainSum =
                        gainSum.add(change);

            } else if (change.compareTo(BigDecimal.ZERO) < 0) {

                lossSum =
                        lossSum.add(
                                change.abs()
                        );
            }
        }

        BigDecimal averageGain =
                gainSum.divide(
                        BigDecimal.valueOf(period),
                        CALCULATION_SCALE,
                        RoundingMode.HALF_UP
                );

        BigDecimal averageLoss =
                lossSum.divide(
                        BigDecimal.valueOf(period),
                        CALCULATION_SCALE,
                        RoundingMode.HALF_UP
                );

        /*
         * ---------------------------------------------------------
         * Step 2: Wilder's smoothing
         * ---------------------------------------------------------
         *
         * Average Gain =
         *
         * ((Previous Average Gain × (period - 1))
         *  + Current Gain) / period
         *
         * Same formula for Average Loss.
         */
        for (int i = period + 1;
             i < candles.size();
             i++) {

            BigDecimal currentClose =
                    getClose(candles.get(i));

            BigDecimal previousClose =
                    getClose(candles.get(i - 1));

            if (currentClose == null
                    || previousClose == null) {

                return null;
            }

            BigDecimal change =
                    currentClose.subtract(
                            previousClose
                    );

            BigDecimal gain =
                    change.compareTo(BigDecimal.ZERO) > 0
                            ? change
                            : BigDecimal.ZERO;

            BigDecimal loss =
                    change.compareTo(BigDecimal.ZERO) < 0
                            ? change.abs()
                            : BigDecimal.ZERO;

            averageGain =
                    averageGain
                            .multiply(
                                    BigDecimal.valueOf(
                                            period - 1
                                    )
                            )
                            .add(gain)
                            .divide(
                                    BigDecimal.valueOf(period),
                                    CALCULATION_SCALE,
                                    RoundingMode.HALF_UP
                            );

            averageLoss =
                    averageLoss
                            .multiply(
                                    BigDecimal.valueOf(
                                            period - 1
                                    )
                            )
                            .add(loss)
                            .divide(
                                    BigDecimal.valueOf(period),
                                    CALCULATION_SCALE,
                                    RoundingMode.HALF_UP
                            );
        }

        /*
         * ---------------------------------------------------------
         * Step 3: Handle zero-loss scenarios
         * ---------------------------------------------------------
         *
         * If there has been no loss:
         *
         * Average Loss = 0
         *
         * RSI = 100 when there is gain.
         */
        if (averageLoss.compareTo(BigDecimal.ZERO) == 0) {

            if (averageGain.compareTo(BigDecimal.ZERO) == 0) {

                /*
                 * No movement at all.
                 *
                 * RSI is conventionally treated as 50.
                 */
                return BigDecimal.valueOf(50)
                        .setScale(
                                RESULT_SCALE,
                                RoundingMode.HALF_UP
                        );
            }

            return ONE_HUNDRED.setScale(
                    RESULT_SCALE,
                    RoundingMode.HALF_UP
            );
        }

        /*
         * ---------------------------------------------------------
         * Step 4: Relative Strength
         * ---------------------------------------------------------
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
         * ---------------------------------------------------------
         * Step 5: RSI
         * ---------------------------------------------------------
         *
         * RSI = 100 - (100 / (1 + RS))
         */
        BigDecimal rsi =
                ONE_HUNDRED.subtract(
                        ONE_HUNDRED.divide(
                                BigDecimal.ONE.add(
                                        relativeStrength
                                ),
                                CALCULATION_SCALE,
                                RoundingMode.HALF_UP
                        )
                );

        return rsi.setScale(
                RESULT_SCALE,
                RoundingMode.HALF_UP
        );
    }

    private BigDecimal getClose(
            MarketCandle candle) {

        if (candle == null) {
            return null;
        }

        return candle.close();
    }
}