package com.Dashboard.myTradingPlatform.market.analytics.calculator;

import com.Dashboard.myTradingPlatform.market.model.MarketCandle;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class SmaCalculator {

    private static final int RESULT_SCALE = 4;
    private static final RoundingMode ROUNDING_MODE =
            RoundingMode.HALF_UP;

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

        BigDecimal sum =
                BigDecimal.ZERO;

        /*
         * =====================================================
         * LAST N CANDLES
         * =====================================================
         *
         * Candles are expected in chronological order:
         *
         * oldest -> newest
         *
         * For SMA we only use the latest 'period' candles.
         */
        int startIndex =
                candles.size() - period;

        for (int i = startIndex;
             i < candles.size();
             i++) {

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

        /*
         * =====================================================
         * SMA
         * =====================================================
         *
         * SMA = Sum of closing prices / period
         */
        return sum.divide(
                BigDecimal.valueOf(period),
                RESULT_SCALE,
                ROUNDING_MODE
        );
    }
}