package com.Dashboard.myTradingPlatform.market.cache;

import com.Dashboard.myTradingPlatform.market.model.MarketCandle;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MarketCandleCache {

    private static final int MAX_CANDLES = 500;

    private final Map<String, Deque<MarketCandle>> candleCache =
            new ConcurrentHashMap<>();

    public void put(MarketCandle candle) {
        String key = candle.instrumentKey() + "_" + candle.timeframe();

        Deque<MarketCandle> candles = candleCache.computeIfAbsent(
                key,
                k -> new ArrayDeque<>()
        );

        synchronized (candles) {
            if (!candles.isEmpty()) {
                MarketCandle latest = candles.peekLast();

                if (latest.timestamp().equals(candle.timestamp())) {
                    candles.removeLast();
                }
            }

            candles.addLast(candle);

            while (candles.size() > MAX_CANDLES) {
                candles.removeFirst();
            }
        }
    }

    public MarketCandle getLatest(
            String instrumentKey,
            String timeframe) {

        String key =
                instrumentKey
                        + "_"
                        + timeframe;

        Deque<MarketCandle> candles =
                candleCache.get(key);

        if (candles == null || candles.isEmpty()) {
            return null;
        }

        return candles.peekLast();
    }

    public List<MarketCandle> getCandles(
            String instrumentKey,
            String timeframe) {

        String key =
                instrumentKey
                        + "_"
                        + timeframe;

        Deque<MarketCandle> candles =
                candleCache.get(key);

        if (candles == null) {
            return List.of();
        }

        return List.copyOf(candles);
    }
}