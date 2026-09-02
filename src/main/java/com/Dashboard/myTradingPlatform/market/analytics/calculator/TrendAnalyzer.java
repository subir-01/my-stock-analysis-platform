package com.Dashboard.myTradingPlatform.market.analytics.calculator;

import com.Dashboard.myTradingPlatform.market.analytics.model.MarketAnalysis;
import com.Dashboard.myTradingPlatform.market.analytics.model.TrendDirection;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TrendAnalyzer {

    private static final BigDecimal RSI_BULLISH_LEVEL =
            BigDecimal.valueOf(55);

    private static final BigDecimal RSI_BEARISH_LEVEL =
            BigDecimal.valueOf(45);

    public TrendResult analyze(
            MarketAnalysis analysis) {

        if (analysis == null
                || analysis.price() == null) {

            return new TrendResult(
                    TrendDirection.NEUTRAL,
                    0,
                    0
            );
        }

        int bullishScore = 0;
        int bearishScore = 0;

        BigDecimal price = analysis.price();
        BigDecimal sma20 = analysis.sma20();
        BigDecimal ema20 = analysis.ema20();
        BigDecimal ema50 = analysis.ema50();
        BigDecimal rsi14 = analysis.rsi14();
        BigDecimal vwap = analysis.vwap();

        /*
         * =====================================================
         * 1. PRICE VS SMA20
         * =====================================================
         */
        if (sma20 != null) {

            if (price.compareTo(sma20) > 0) {

                bullishScore++;

            } else if (price.compareTo(sma20) < 0) {

                bearishScore++;
            }
        }

        /*
         * =====================================================
         * 2. PRICE VS EMA20
         * =====================================================
         */
        if (ema20 != null) {

            if (price.compareTo(ema20) > 0) {

                bullishScore++;

            } else if (price.compareTo(ema20) < 0) {

                bearishScore++;
            }
        }

        /*
         * =====================================================
         * 3. EMA20 VS EMA50
         * =====================================================
         */
        if (ema20 != null
                && ema50 != null) {

            if (ema20.compareTo(ema50) > 0) {

                bullishScore++;

            } else if (ema20.compareTo(ema50) < 0) {

                bearishScore++;
            }
        }

        /*
         * =====================================================
         * 4. PRICE VS VWAP
         * =====================================================
         */
        if (vwap != null) {

            if (price.compareTo(vwap) > 0) {

                bullishScore++;

            } else if (price.compareTo(vwap) < 0) {

                bearishScore++;
            }
        }

        /*
         * =====================================================
         * 5. RSI
         * =====================================================
         *
         * RSI > 70 is NOT automatically bearish.
         *
         * Strong bullish trends can remain overbought.
         *
         * Therefore:
         *
         * RSI >= 55 -> bullish
         * RSI <= 45 -> bearish
         * 45 < RSI < 55 -> neutral
         */
        if (rsi14 != null) {

            if (rsi14.compareTo(
                    RSI_BULLISH_LEVEL
            ) >= 0) {

                bullishScore++;

            } else if (rsi14.compareTo(
                    RSI_BEARISH_LEVEL
            ) <= 0) {

                bearishScore++;
            }
        }

        /*
         * =====================================================
         * DETERMINE FINAL TREND
         * =====================================================
         */
        TrendDirection direction;

        if (bullishScore > bearishScore) {

            direction =
                    TrendDirection.BULLISH;

        } else if (bearishScore > bullishScore) {

            direction =
                    TrendDirection.BEARISH;

        } else {

            direction =
                    TrendDirection.NEUTRAL;
        }

        return new TrendResult(
                direction,
                bullishScore,
                bearishScore
        );
    }

    /*
     * =========================================================
     * TREND RESULT
     * =========================================================
     */
    public record TrendResult(

            TrendDirection direction,

            int bullishScore,

            int bearishScore

    ) {
    }
}