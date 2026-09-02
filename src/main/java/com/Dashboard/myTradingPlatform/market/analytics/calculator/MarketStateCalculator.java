package com.Dashboard.myTradingPlatform.market.analytics.calculator;

import com.Dashboard.myTradingPlatform.market.analytics.model.MarketAnalysis;
import com.Dashboard.myTradingPlatform.market.analytics.model.MarketRegime;
import com.Dashboard.myTradingPlatform.market.analytics.model.MomentumCondition;
import com.Dashboard.myTradingPlatform.market.analytics.model.MomentumState;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MarketStateCalculator {

    /*
     * =========================================================
     * CALCULATE MARKET STATE
     * =========================================================
     */
    public MarketStateResult calculate(
            MarketAnalysis analysis) {

        if (analysis == null) {

            return new MarketStateResult(
                    MarketRegime.UNKNOWN,
                    MomentumState.NEUTRAL,
                    MomentumCondition.UNKNOWN
            );
        }

        int trendScore =
                calculateTrendScore(analysis);

        MarketRegime regime =
                determineRegime(trendScore);

        MomentumState momentum =
                determineMomentum(analysis.rsi14());

        MomentumCondition momentumCondition =
                determineMomentumCondition(
                        analysis.rsi14()
                );

        return new MarketStateResult(
                regime,
                momentum,
                momentumCondition
        );
    }

    /*
     * =========================================================
     * TREND SCORE
     * =========================================================
     *
     * Maximum:
     *
     * +5 = strongly bullish
     * -5 = strongly bearish
     */
    private int calculateTrendScore(
            MarketAnalysis analysis) {

        BigDecimal price =
                analysis.price();

        int score = 0;

        /*
         * PRICE vs SMA20
         */
        if (price != null
                && analysis.sma20() != null) {

            if (price.compareTo(
                    analysis.sma20()
            ) > 0) {

                score++;

            } else if (price.compareTo(
                    analysis.sma20()
            ) < 0) {

                score--;
            }
        }

        /*
         * PRICE vs EMA20
         */
        if (price != null
                && analysis.ema20() != null) {

            if (price.compareTo(
                    analysis.ema20()
            ) > 0) {

                score++;

            } else if (price.compareTo(
                    analysis.ema20()
            ) < 0) {

                score--;
            }
        }

        /*
         * EMA20 vs EMA50
         */
        if (analysis.ema20() != null
                && analysis.ema50() != null) {

            if (analysis.ema20().compareTo(
                    analysis.ema50()
            ) > 0) {

                score++;

            } else if (analysis.ema20().compareTo(
                    analysis.ema50()
            ) < 0) {

                score--;
            }
        }

        /*
         * PRICE vs VWAP
         */
        if (price != null
                && analysis.vwap() != null) {

            if (price.compareTo(
                    analysis.vwap()
            ) > 0) {

                score++;

            } else if (price.compareTo(
                    analysis.vwap()
            ) < 0) {

                score--;
            }
        }

        /*
         * RSI > 50 / < 50
         */
        if (analysis.rsi14() != null) {

            if (analysis.rsi14().compareTo(
                    BigDecimal.valueOf(50)
            ) > 0) {

                score++;

            } else if (analysis.rsi14().compareTo(
                    BigDecimal.valueOf(50)
            ) < 0) {

                score--;
            }
        }

        return score;
    }

    /*
     * =========================================================
     * MARKET REGIME
     * =========================================================
     */
    private MarketRegime determineRegime(
            int score) {

        if (score >= 4) {

            return MarketRegime.STRONGLY_BULLISH;
        }

        if (score >= 2) {

            return MarketRegime.BULLISH;
        }

        if (score <= -4) {

            return MarketRegime.STRONGLY_BEARISH;
        }

        if (score <= -2) {

            return MarketRegime.BEARISH;
        }

        return MarketRegime.NEUTRAL;
    }

    /*
     * =========================================================
     * MOMENTUM STATE
     * =========================================================
     */
    private MomentumState determineMomentum(
            BigDecimal rsi) {

        if (rsi == null) {

            return MomentumState.NEUTRAL;
        }

        if (rsi.compareTo(
                BigDecimal.valueOf(70)
        ) >= 0) {

            return MomentumState.VERY_STRONG_BULLISH;
        }

        if (rsi.compareTo(
                BigDecimal.valueOf(60)
        ) >= 0) {

            return MomentumState.STRONG_BULLISH;
        }

        if (rsi.compareTo(
                BigDecimal.valueOf(50)
        ) > 0) {

            return MomentumState.BULLISH;
        }

        if (rsi.compareTo(
                BigDecimal.valueOf(30)
        ) <= 0) {

            return MomentumState.VERY_STRONG_BEARISH;
        }

        if (rsi.compareTo(
                BigDecimal.valueOf(40)
        ) <= 0) {

            return MomentumState.STRONG_BEARISH;
        }

        return MomentumState.BEARISH;
    }

    /*
     * =========================================================
     * MOMENTUM CONDITION
     * =========================================================
     */
    private MomentumCondition determineMomentumCondition(
            BigDecimal rsi) {

        if (rsi == null) {

            return MomentumCondition.UNKNOWN;
        }

        /*
         * RSI >= 90
         */
        if (rsi.compareTo(
                BigDecimal.valueOf(90)
        ) >= 0) {

            return MomentumCondition.EXTREME_OVERBOUGHT;
        }

        /*
         * RSI >= 70
         */
        if (rsi.compareTo(
                BigDecimal.valueOf(70)
        ) >= 0) {

            return MomentumCondition.OVERBOUGHT;
        }

        /*
         * RSI >= 60
         */
        if (rsi.compareTo(
                BigDecimal.valueOf(60)
        ) >= 0) {

            return MomentumCondition.STRONG;
        }

        /*
         * RSI >= 40
         */
        if (rsi.compareTo(
                BigDecimal.valueOf(40)
        ) >= 0) {

            return MomentumCondition.NORMAL;
        }

        /*
         * RSI >= 30
         */
        if (rsi.compareTo(
                BigDecimal.valueOf(30)
        ) >= 0) {

            return MomentumCondition.WEAK;
        }

        /*
         * RSI >= 20
         */
        if (rsi.compareTo(
                BigDecimal.valueOf(20)
        ) >= 0) {

            return MomentumCondition.OVERSOLD;
        }

        return MomentumCondition.EXTREME_OVERSOLD;
    }

    /*
     * =========================================================
     * RESULT
     * =========================================================
     */
    public record MarketStateResult(
            MarketRegime regime,
            MomentumState momentum,
            MomentumCondition momentumCondition
    ) {
    }
}