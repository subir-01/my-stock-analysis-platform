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

        if (candles == null
                || candles.size() < 3
                || currentPrice == null) {

            return null;
        }

        BigDecimal support = null;

        /*
         * Start from the second candle because we need:
         *
         * previous candle
         * current candle
         * next candle
         *
         * to identify a swing low.
         */
        for (int i = 1; i < candles.size() - 1; i++) {

            MarketCandle previous =
                    candles.get(i - 1);

            MarketCandle current =
                    candles.get(i);

            MarketCandle next =
                    candles.get(i + 1);

            if (previous == null
                    || current == null
                    || next == null
                    || previous.low() == null
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
             * Swing Low:
             *
             *       previous
             *          ↓
             *
             *        current
             *           ↓
             *          LOW
             *
             *        next
             */
            boolean isSwingLow =
                    currentLow.compareTo(previousLow) <= 0
                            && currentLow.compareTo(nextLow) <= 0;

            /*
             * Support must be below the current market price.
             */
            boolean belowCurrentPrice =
                    currentLow.compareTo(currentPrice) < 0;

            if (!isSwingLow || !belowCurrentPrice) {
                continue;
            }

            /*
             * Select the closest support below
             * the current market price.
             *
             * Example:
             *
             * Current price = 1287
             *
             * Swing lows:
             * 1250
             * 1270
             * 1280
             *
             * Support = 1280
             */
            if (support == null
                    || currentLow.compareTo(support) > 0) {

                support = currentLow;
            }
        }

        return support;
    }
}