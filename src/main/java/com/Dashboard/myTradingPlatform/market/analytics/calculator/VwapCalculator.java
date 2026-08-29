package com.Dashboard.myTradingPlatform.market.analytics.calculator;

import com.Dashboard.myTradingPlatform.market.model.MarketCandle;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.LocalDate;
import java.util.List;

@Component
public class VwapCalculator {

    private static final int CALCULATION_SCALE = 10;
    private static final int RESULT_SCALE = 4;

    /*
     * Indian market timezone.
     */
    private static final ZoneId MARKET_ZONE =
            ZoneId.of("Asia/Kolkata");

    public BigDecimal calculate(
            List<MarketCandle> candles) {

        if (candles == null
                || candles.isEmpty()) {

            return null;
        }

        /*
         * ---------------------------------------------------------
         * Determine the latest trading session.
         * ---------------------------------------------------------
         *
         * The latest candle determines which session's VWAP
         * we need to calculate.
         */
        MarketCandle latest =
                candles.get(
                        candles.size() - 1
                );

        if (latest == null
                || latest.timestamp() == null) {

            return null;
        }

        LocalDate latestSession =
                getTradingDate(
                        latest.timestamp()
                );

        BigDecimal totalPriceVolume =
                BigDecimal.ZERO;

        long totalVolume = 0;

        /*
         * ---------------------------------------------------------
         * Process only candles belonging to the latest session.
         * ---------------------------------------------------------
         */
        for (MarketCandle candle : candles) {

            if (candle == null
                    || candle.timestamp() == null
                    || candle.high() == null
                    || candle.low() == null
                    || candle.close() == null
                    || candle.volume() == null) {

                continue;
            }

            LocalDate candleSession =
                    getTradingDate(
                            candle.timestamp()
                    );

            /*
             * Ignore previous trading sessions.
             */
            if (!latestSession.equals(
                    candleSession)) {

                continue;
            }

            long volume =
                    candle.volume();

            /*
             * Ignore candles without usable volume.
             */
            if (volume <= 0) {
                continue;
            }

            /*
             * Typical Price =
             *
             * (High + Low + Close) / 3
             */
            BigDecimal typicalPrice =
                    candle.high()
                            .add(candle.low())
                            .add(candle.close())
                            .divide(
                                    BigDecimal.valueOf(3),
                                    CALCULATION_SCALE,
                                    RoundingMode.HALF_UP
                            );

            /*
             * Typical Price × Volume
             */
            BigDecimal priceVolume =
                    typicalPrice.multiply(
                            BigDecimal.valueOf(volume)
                    );

            totalPriceVolume =
                    totalPriceVolume.add(
                            priceVolume
                    );

            totalVolume += volume;
        }

        /*
         * No usable volume.
         */
        if (totalVolume <= 0) {
            return null;
        }

        /*
         * VWAP =
         *
         * Σ(Typical Price × Volume)
         * /
         * ΣVolume
         */
        return totalPriceVolume.divide(
                BigDecimal.valueOf(totalVolume),
                RESULT_SCALE,
                RoundingMode.HALF_UP
        );
    }

    private LocalDate getTradingDate(
            Instant timestamp) {

        return timestamp
                .atZone(MARKET_ZONE)
                .toLocalDate();
    }
}