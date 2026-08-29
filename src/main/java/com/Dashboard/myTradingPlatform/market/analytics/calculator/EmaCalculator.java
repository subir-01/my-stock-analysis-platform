package com.Dashboard.myTradingPlatform.market.analytics.calculator;

import com.Dashboard.myTradingPlatform.market.model.MarketCandle;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class EmaCalculator {

    private static final int CALCULATION_SCALE = 10;
    private static final int RESULT_SCALE = 4;

    public BigDecimal calculate(
            List<MarketCandle> candles,
            int period) {

        if (candles == null
                || candles.isEmpty()
                || period <= 0
                || candles.size() < period) {

            return null;
        }

        /*
         * ---------------------------------------------------------
         * Step 1: Calculate initial SMA
         * ---------------------------------------------------------
         *
         * EMA starts with an SMA of the first 'period' candles.
         */
        BigDecimal sum = BigDecimal.ZERO;

        for (int i = 0; i < period; i++) {

            MarketCandle candle =
                    candles.get(i);

            if (candle == null
                    || candle.close() == null) {

                return null;
            }

            sum = sum.add(
                    candle.close()
            );
        }

        BigDecimal ema =
                sum.divide(
                        BigDecimal.valueOf(period),
                        CALCULATION_SCALE,
                        RoundingMode.HALF_UP
                );

        /*
         * ---------------------------------------------------------
         * Step 2: EMA multiplier
         * ---------------------------------------------------------
         *
         * Multiplier = 2 / (period + 1)
         */
        BigDecimal multiplier =
                BigDecimal.valueOf(2)
                        .divide(
                                BigDecimal.valueOf(period + 1),
                                CALCULATION_SCALE,
                                RoundingMode.HALF_UP
                        );

        /*
         * ---------------------------------------------------------
         * Step 3: Calculate EMA
         * ---------------------------------------------------------
         *
         * EMA =
         *
         * (Close - Previous EMA) * Multiplier
         * + Previous EMA
         */
        for (int i = period; i < candles.size(); i++) {

            MarketCandle candle =
                    candles.get(i);

            if (candle == null
                    || candle.close() == null) {

                return null;
            }

            BigDecimal close =
                    candle.close();

            ema =
                    close
                            .subtract(ema)
                            .multiply(multiplier)
                            .add(ema);
        }

        /*
         * Return consistent 4-decimal precision.
         */
        return ema.setScale(
                RESULT_SCALE,
                RoundingMode.HALF_UP
        );
    }
}