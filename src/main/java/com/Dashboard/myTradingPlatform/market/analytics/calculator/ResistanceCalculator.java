package com.Dashboard.myTradingPlatform.market.analytics.calculator;

import com.Dashboard.myTradingPlatform.market.model.MarketCandle;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class ResistanceCalculator {

    public BigDecimal calculate(
            List<MarketCandle> candles,
            BigDecimal currentPrice) {

        if (candles == null
                || candles.size() < 3
                || currentPrice == null) {

            return null;
        }

        BigDecimal resistance = null;

        /*
         * We need one candle before and one candle
         * after the current candle to identify
         * a swing high.
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
                    || previous.high() == null
                    || current.high() == null
                    || next.high() == null) {

                continue;
            }

            BigDecimal previousHigh =
                    previous.high();

            BigDecimal currentHigh =
                    current.high();

            BigDecimal nextHigh =
                    next.high();

            /*
             * Swing High:
             *
             * previous high
             *       ↑
             *
             * current high
             *       ↑
             *      HIGH
             *
             * next high
             */
            boolean isSwingHigh =
                    currentHigh.compareTo(previousHigh) >= 0
                            && currentHigh.compareTo(nextHigh) >= 0;

            /*
             * Resistance must be above
             * the current market price.
             */
            boolean aboveCurrentPrice =
                    currentHigh.compareTo(currentPrice) > 0;

            if (!isSwingHigh || !aboveCurrentPrice) {
                continue;
            }

            /*
             * Select the closest resistance above
             * the current market price.
             *
             * Example:
             *
             * Current price = 1287
             *
             * Swing highs:
             * 1295
             * 1305
             * 1320
             *
             * Resistance = 1295
             */
            if (resistance == null
                    || currentHigh.compareTo(resistance) < 0) {

                resistance = currentHigh;
            }
        }

        return resistance;
    }
}