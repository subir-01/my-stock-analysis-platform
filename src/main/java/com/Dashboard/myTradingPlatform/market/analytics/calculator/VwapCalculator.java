package com.Dashboard.myTradingPlatform.market.analytics.calculator;

import com.Dashboard.myTradingPlatform.market.model.MarketCandle;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class VwapCalculator {

    private static final int CALCULATION_SCALE = 10;
    private static final int RESULT_SCALE = 4;

    public BigDecimal calculate(
            List<MarketCandle> candles) {

        /*
         * =====================================================
         * VALIDATION
         * =====================================================
         */
        if (candles == null
                || candles.isEmpty()) {

            return null;
        }

        BigDecimal totalPriceVolume =
                BigDecimal.ZERO;

        BigDecimal totalVolume =
                BigDecimal.ZERO;

        /*
         * =====================================================
         * VWAP CALCULATION
         * =====================================================
         *
         * Typical Price:
         *
         * (High + Low + Close) / 3
         *
         * VWAP:
         *
         * Sum(Typical Price × Volume)
         * ----------------------------
         *          Sum(Volume)
         *
         * Candles are expected in chronological order:
         *
         * oldest -> newest
         */
        for (MarketCandle candle : candles) {

            if (candle == null) {
                continue;
            }

            /*
             * Ignore invalid OHLC candles.
             */
            if (candle.high() == null
                    || candle.low() == null
                    || candle.close() == null) {

                continue;
            }

            /*
             * Ignore candles without usable volume.
             */
            if (candle.volume() <= 0) {
                continue;
            }

            BigDecimal typicalPrice =
                    candle.high()
                            .add(candle.low())
                            .add(candle.close())
                            .divide(
                                    BigDecimal.valueOf(3),
                                    CALCULATION_SCALE,
                                    RoundingMode.HALF_UP
                            );

            BigDecimal volume =
                    BigDecimal.valueOf(
                            candle.volume()
                    );

            totalPriceVolume =
                    totalPriceVolume.add(
                            typicalPrice.multiply(
                                    volume
                            )
                    );

            totalVolume =
                    totalVolume.add(
                            volume
                    );
        }

        /*
         * =====================================================
         * NO VALID VOLUME
         * =====================================================
         */
        if (totalVolume.compareTo(
                BigDecimal.ZERO
        ) == 0) {

            return null;
        }

        /*
         * =====================================================
         * FINAL VWAP
         * =====================================================
         */
        return totalPriceVolume
                .divide(
                        totalVolume,
                        RESULT_SCALE,
                        RoundingMode.HALF_UP
                );
    }
}