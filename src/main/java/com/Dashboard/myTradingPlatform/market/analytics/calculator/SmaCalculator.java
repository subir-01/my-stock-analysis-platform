package com.Dashboard.myTradingPlatform.market.analytics.calculator;

import com.Dashboard.myTradingPlatform.market.model.MarketCandle;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class SmaCalculator {

    private static final int SCALE = 4;

    public BigDecimal calculate(
            List<MarketCandle> candles,
            int period) {

        if (candles == null
                || candles.isEmpty()
                || period <= 0
                || candles.size() < period) {

            return null;
        }

        BigDecimal sum = BigDecimal.ZERO;

        int startIndex =
                candles.size() - period;

        for (int i = startIndex; i < candles.size(); i++) {

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

        return sum.divide(
                BigDecimal.valueOf(period),
                SCALE,
                RoundingMode.HALF_UP
        );
    }
}