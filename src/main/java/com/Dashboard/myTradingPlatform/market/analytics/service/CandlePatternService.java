package com.Dashboard.myTradingPlatform.market.analytics.service;

import com.Dashboard.myTradingPlatform.market.analytics.calculator.CandlePatternCalculator;
import com.Dashboard.myTradingPlatform.market.analytics.model.CandlePatternAnalysis;
import com.Dashboard.myTradingPlatform.market.cache.MarketCandleCache;
import com.Dashboard.myTradingPlatform.market.model.MarketCandle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class CandlePatternService {

    private static final int PATTERN_CANDLE_COUNT = 5;

    private final MarketCandleCache marketCandleCache;
    private final CandlePatternCalculator candlePatternCalculator;

    public CandlePatternService(
            MarketCandleCache marketCandleCache,
            CandlePatternCalculator candlePatternCalculator) {

        this.marketCandleCache = marketCandleCache;
        this.candlePatternCalculator = candlePatternCalculator;
    }

    public CandlePatternAnalysis analyze(
            String instrumentKey,
            String timeframe) {

        validate(
                instrumentKey,
                timeframe
        );

        List<MarketCandle> candles =
                marketCandleCache.getLastCandles(
                        instrumentKey,
                        timeframe,
                        PATTERN_CANDLE_COUNT
                );

        if (candles == null
                || candles.isEmpty()) {

            log.warn(
                    "No candles available for pattern analysis: instrument={}, timeframe={}",
                    instrumentKey,
                    timeframe
            );

            return null;
        }

        CandlePatternAnalysis result =
                candlePatternCalculator.calculate(
                        candles
                );

        if (result == null) {

            log.warn(
                    "Unable to determine candle pattern: instrument={}, timeframe={}",
                    instrumentKey,
                    timeframe
            );

            return null;
        }

        log.info(
                "Candle pattern analysis completed: instrument={}, timeframe={}, pattern={}, direction={}, strength={}, score={}",
                instrumentKey,
                timeframe,
                result.pattern(),
                result.direction(),
                result.strength(),
                result.score()
        );

        return result;
    }

    private void validate(
            String instrumentKey,
            String timeframe) {

        if (instrumentKey == null
                || instrumentKey.isBlank()) {

            throw new IllegalArgumentException(
                    "Instrument key must not be null or blank"
            );
        }

        if (timeframe == null
                || timeframe.isBlank()) {

            throw new IllegalArgumentException(
                    "Timeframe must not be null or blank"
            );
        }
    }
}