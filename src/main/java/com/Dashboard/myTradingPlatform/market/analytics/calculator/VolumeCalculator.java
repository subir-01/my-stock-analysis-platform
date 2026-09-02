package com.Dashboard.myTradingPlatform.market.analytics.calculator;

import com.Dashboard.myTradingPlatform.market.analytics.model.VolumeAnalysis;
import com.Dashboard.myTradingPlatform.market.analytics.model.VolumeCondition;
import com.Dashboard.myTradingPlatform.market.model.MarketCandle;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class VolumeCalculator {

    /*
     * =========================================================
     * VOLUME THRESHOLDS
     * =========================================================
     *
     * Ratio = Current Volume / Average Volume
     *
     * 2.00 = 200% of average
     * 1.50 = 150% of average
     * 0.80 = 80% of average
     * 0.50 = 50% of average
     */
    private static final BigDecimal VERY_HIGH_RATIO =
            new BigDecimal("2.00");

    private static final BigDecimal HIGH_RATIO =
            new BigDecimal("1.50");

    private static final BigDecimal NORMAL_RATIO =
            new BigDecimal("0.80");

    private static final BigDecimal LOW_RATIO =
            new BigDecimal("0.50");

    /*
     * =========================================================
     * MAIN CALCULATION
     * =========================================================
     */
    public VolumeAnalysis calculate(
            String timeframe,
            List<MarketCandle> candles) {

        if (candles == null
                || candles.size() < 2) {

            return new VolumeAnalysis(
                    timeframe,
                    null,
                    null,
                    null,
                    VolumeCondition.UNKNOWN,
                    false
            );
        }

        /*
         * =====================================================
         * LATEST CANDLE
         * =====================================================
         */
        MarketCandle latest =
                candles.get(
                        candles.size() - 1
                );

        if (latest == null
                || latest.volume() == null
                || latest.volume() < 0) {

            return new VolumeAnalysis(
                    timeframe,
                    null,
                    null,
                    null,
                    VolumeCondition.UNKNOWN,
                    false
            );
        }

        /*
         * =====================================================
         * CURRENT VOLUME
         * =====================================================
         *
         * MarketCandle.volume() is Long.
         *
         * Convert it to BigDecimal for calculations.
         */
        BigDecimal currentVolume =
                BigDecimal.valueOf(
                        latest.volume()
                );

        /*
         * =====================================================
         * CALCULATE AVERAGE VOLUME
         * =====================================================
         *
         * We deliberately exclude the latest candle.
         *
         * This prevents the current candle from influencing
         * its own volume baseline.
         */
        BigDecimal totalVolume =
                BigDecimal.ZERO;

        int validCount = 0;

        for (int i = 0;
             i < candles.size() - 1;
             i++) {

            MarketCandle candle =
                    candles.get(i);

            if (candle == null
                    || candle.volume() == null
                    || candle.volume() < 0) {

                continue;
            }

            BigDecimal candleVolume =
                    BigDecimal.valueOf(
                            candle.volume()
                    );

            totalVolume =
                    totalVolume.add(
                            candleVolume
                    );

            validCount++;
        }

        /*
         * =====================================================
         * NO VALID VOLUME DATA
         * =====================================================
         */
        if (validCount == 0) {

            return new VolumeAnalysis(
                    timeframe,
                    currentVolume,
                    null,
                    null,
                    VolumeCondition.UNKNOWN,
                    false
            );
        }

        /*
         * =====================================================
         * AVERAGE VOLUME
         * =====================================================
         */
        BigDecimal averageVolume =
                totalVolume.divide(
                        BigDecimal.valueOf(validCount),
                        8,
                        RoundingMode.HALF_UP
                );

        /*
         * =====================================================
         * AVERAGE VOLUME = ZERO
         * =====================================================
         */
        if (averageVolume.compareTo(
                BigDecimal.ZERO
        ) == 0) {

            return new VolumeAnalysis(
                    timeframe,
                    currentVolume,
                    averageVolume,
                    null,
                    VolumeCondition.UNKNOWN,
                    false
            );
        }

        /*
         * =====================================================
         * VOLUME RATIO
         * =====================================================
         *
         * Example:
         *
         * Current volume = 150,000
         * Average volume = 100,000
         *
         * Ratio = 1.50
         */
        BigDecimal volumeRatio =
                currentVolume.divide(
                        averageVolume,
                        4,
                        RoundingMode.HALF_UP
                );

        /*
         * =====================================================
         * VOLUME CONDITION
         * =====================================================
         */
        VolumeCondition condition =
                determineCondition(
                        volumeRatio
                );

        /*
         * =====================================================
         * VOLUME CONFIRMATION
         * =====================================================
         *
         * 1.50x or more of average volume is considered
         * strong volume confirmation.
         */
        boolean volumeConfirmed =
                volumeRatio.compareTo(
                        HIGH_RATIO
                ) >= 0;

        return new VolumeAnalysis(
                timeframe,
                currentVolume,
                averageVolume,
                volumeRatio,
                condition,
                volumeConfirmed
        );
    }

    /*
     * =========================================================
     * DETERMINE VOLUME CONDITION
     * =========================================================
     */
    private VolumeCondition determineCondition(
            BigDecimal ratio) {

        if (ratio == null) {

            return VolumeCondition.UNKNOWN;
        }

        if (ratio.compareTo(
                VERY_HIGH_RATIO
        ) >= 0) {

            return VolumeCondition.VERY_HIGH;
        }

        if (ratio.compareTo(
                HIGH_RATIO
        ) >= 0) {

            return VolumeCondition.HIGH;
        }

        if (ratio.compareTo(
                NORMAL_RATIO
        ) >= 0) {

            return VolumeCondition.NORMAL;
        }

        if (ratio.compareTo(
                LOW_RATIO
        ) >= 0) {

            return VolumeCondition.LOW;
        }

        return VolumeCondition.VERY_LOW;
    }
}