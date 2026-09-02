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

        BigDecimal resistance = null;

        /*
         * =====================================================
         * FIND SWING HIGHS
         * =====================================================
         *
         * A candle is treated as a swing high when:
         *
         * current high >= previous high
         * AND
         * current high >= next high
         *
         * From all swing highs above the current price,
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

            if (previous.high() == null
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
             * Check swing-high structure.
             */
            boolean isSwingHigh =
                    currentHigh.compareTo(previousHigh) >= 0
                            && currentHigh.compareTo(nextHigh) >= 0;

            if (!isSwingHigh) {
                continue;
            }

            /*
             * Resistance must be above current price.
             */
            if (currentHigh.compareTo(currentPrice) <= 0) {
                continue;
            }

            /*
             * Select nearest resistance.
             */
            if (resistance == null
                    || currentHigh.compareTo(resistance) < 0) {

                resistance = currentHigh;
            }
        }

        return resistance;
    }
}