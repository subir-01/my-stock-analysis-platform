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

        /*
         * =====================================================
         * VALIDATION
         * =====================================================
         */
        if (candles == null
                || candles.isEmpty()
                || period <= 0
                || candles.size() < period) {

            return null;
        }

        /*
         * =====================================================
         * INITIAL EMA
         * =====================================================
         *
         * The first EMA value is initialized using SMA
         * of the first 'period' candles.
         *
         * Example for EMA20:
         *
         * First 20 candles
         *        ↓
         *      SMA20
         *        ↓
         * Initial EMA
         */
        BigDecimal sum =
                BigDecimal.ZERO;

        for (int i = 0; i < period; i++) {

            MarketCandle candle =
                    candles.get(i);

            if (candle == null
                    || candle.close() == null) {

                return null;
            }

            sum =
                    sum.add(
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
         * =====================================================
         * EMA MULTIPLIER
         * =====================================================
         *
         * Multiplier =
         *
         *       2
         *  -----------
         *   period + 1
         *
         * For EMA20:
         *
         * 2 / 21
         */
        BigDecimal multiplier =
                BigDecimal.valueOf(2)
                        .divide(
                                BigDecimal.valueOf(
                                        period + 1
                                ),
                                CALCULATION_SCALE,
                                RoundingMode.HALF_UP
                        );

        /*
         * =====================================================
         * CALCULATE EMA
         * =====================================================
         *
         * EMA =
         *
         * (Close - Previous EMA) × Multiplier
         * + Previous EMA
         *
         * We continue from candle 'period' until the latest
         * candle.
         */
        for (int i = period;
             i < candles.size();
             i++) {

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
         * =====================================================
         * RESULT
         * =====================================================
         */
        return ema.setScale(
                RESULT_SCALE,
                RoundingMode.HALF_UP
        );
    }
}