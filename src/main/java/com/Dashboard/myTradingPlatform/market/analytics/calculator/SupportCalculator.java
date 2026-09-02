package com.Dashboard.myTradingPlatform.market.analytics.calculator;

import com.Dashboard.myTradingPlatform.market.model.MarketCandle;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class SupportCalculator {

    public BigDecimal calculate(
            List<MarketCandle> candles,
            BigDecimal currentPrice) {

        /*
         * =====================================================
         * VALIDATION
         * =====================================================
         */
        if (candles == null
                || candles.size() < 3
                || currentPrice == null) {

            return null;
        }

        BigDecimal support = null;

        /*
         * =====================================================
         * FIND SWING LOWS
         * =====================================================
         *
         * A candle is treated as a swing low when:
         *
         * current low <= previous low
         * AND
         * current low <= next low
         *
         * From all swing lows below the current price,
         * we select the closest one.
         */
        for (int i = 1;
             i < candles.size() - 1;
             i++) {

            MarketCandle previous =
                    candles.get(i - 1);

            MarketCandle current =
                    candles.get(i);

            MarketCandle next =
                    candles.get(i + 1);

            if (previous == null
                    || current == null
                    || next == null) {

                continue;
            }

            if (previous.low() == null
                    || current.low() == null
                    || next.low() == null) {

                continue;
            }

            BigDecimal previousLow =
                    previous.low();

            BigDecimal currentLow =
                    current.low();

            BigDecimal nextLow =
                    next.low();

            /*
             * Check swing-low structure.
             */
            boolean isSwingLow =
                    currentLow.compareTo(previousLow) <= 0
                            && currentLow.compareTo(nextLow) <= 0;

            if (!isSwingLow) {
                continue;
            }

            /*
             * Support must be below current price.
             */
            if (currentLow.compareTo(currentPrice) >= 0) {
                continue;
            }

            /*
             * Select nearest support.
             */
            if (support == null
                    || currentLow.compareTo(support) > 0) {

                support = currentLow;
            }
        }

        return support;
    }
}