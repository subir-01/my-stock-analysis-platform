package com.Dashboard.myTradingPlatform.market.cache;

import com.Dashboard.myTradingPlatform.market.model.MarketCandle;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MarketCandleCache {

    private final Map<String, Map<String, MarketCandle>> candles =
            new ConcurrentHashMap<>();

    public void update(MarketCandle candle) {
        candles
                .computeIfAbsent(
                        candle.instrumentKey(),
                        key -> new ConcurrentHashMap<>()
                )
                .put(
                        candle.timeframe(),
                        candle
                );
    }

    public MarketCandle get(
            String instrumentKey,
            String timeframe
    ) {
        Map<String, MarketCandle> instrumentCandles =
                candles.get(instrumentKey);

        if (instrumentCandles == null) {
            return null;
        }

        return instrumentCandles.get(timeframe);
    }
}