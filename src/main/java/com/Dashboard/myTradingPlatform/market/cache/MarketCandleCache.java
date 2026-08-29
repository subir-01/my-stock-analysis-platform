package com.Dashboard.myTradingPlatform.market.cache;

import com.Dashboard.myTradingPlatform.market.model.MarketCandle;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MarketCandleCache {

    /*
     * Maximum number of candles maintained for each
     * instrument + timeframe combination.
     *
     * Example:
     *
     * Reliance + I1       -> max 10,000
     * Reliance + 1d       -> max 10,000
     * SGX NIFTY + I1      -> max 10,000
     */
    private static final int MAX_CANDLES = 10_000;

    /*
     * Cache structure:
     *
     * instrument + timeframe
     *          ↓
     * timestamp -> MarketCandle
     *
     * Using timestamp as the key means that if the same
     * candle is received again, it automatically replaces
     * the previous candle.
     */
    private final Map<String, NavigableMap<Instant, MarketCandle>> candleCache =
            new ConcurrentHashMap<>();

    /**
     * Add or update a candle.
     *
     * If a candle with the same timestamp already exists,
     * it will be replaced with the latest version.
     */
    public void put(MarketCandle candle) {

        if (candle == null) {
            return;
        }

        if (candle.instrumentKey() == null
                || candle.instrumentKey().isBlank()) {
            return;
        }

        if (candle.timeframe() == null
                || candle.timeframe().isBlank()) {
            return;
        }

        if (candle.timestamp() == null) {
            return;
        }

        String key =
                buildKey(
                        candle.instrumentKey(),
                        candle.timeframe()
                );

        NavigableMap<Instant, MarketCandle> candles =
                candleCache.computeIfAbsent(
                        key,
                        k -> new TreeMap<>()
                );

        synchronized (candles) {

            /*
             * Same timestamp:
             *
             * Existing candle is replaced.
             *
             * This is important for live candles because
             * Upstox can repeatedly send updates for the
             * same candle.
             */
            candles.put(
                    candle.timestamp(),
                    candle
            );

            /*
             * Keep only the latest MAX_CANDLES.
             */
            while (candles.size() > MAX_CANDLES) {

                candles.pollFirstEntry();
            }
        }
    }

    /**
     * Returns the latest candle.
     */
    public MarketCandle getLatest(
            String instrumentKey,
            String timeframe) {

        NavigableMap<Instant, MarketCandle> candles =
                candleCache.get(
                        buildKey(
                                instrumentKey,
                                timeframe
                        )
                );

        if (candles == null || candles.isEmpty()) {
            return null;
        }

        synchronized (candles) {

            Map.Entry<Instant, MarketCandle> latest =
                    candles.lastEntry();

            if (latest == null) {
                return null;
            }

            return latest.getValue();
        }
    }

    /**
     * Returns all candles in chronological order.
     *
     * Oldest -> Newest
     */
    public List<MarketCandle> getCandles(
            String instrumentKey,
            String timeframe) {

        NavigableMap<Instant, MarketCandle> candles =
                candleCache.get(
                        buildKey(
                                instrumentKey,
                                timeframe
                        )
                );

        if (candles == null || candles.isEmpty()) {
            return List.of();
        }

        synchronized (candles) {

            return new ArrayList<>(
                    candles.values()
            );
        }
    }

    /**
     * Returns the latest requested number of candles.
     *
     * Result is returned in chronological order:
     *
     * Oldest -> Newest
     */
    public List<MarketCandle> getLastCandles(
            String instrumentKey,
            String timeframe,
            int count) {

        if (count <= 0) {
            return List.of();
        }

        NavigableMap<Instant, MarketCandle> candles =
                candleCache.get(
                        buildKey(
                                instrumentKey,
                                timeframe
                        )
                );

        if (candles == null || candles.isEmpty()) {
            return List.of();
        }

        synchronized (candles) {

            int size =
                    Math.min(
                            count,
                            candles.size()
                    );

            List<MarketCandle> result =
                    new ArrayList<>(size);

            /*
             * Iterate from newest to oldest.
             */
            var iterator =
                    candles.descendingMap()
                            .values()
                            .iterator();

            while (iterator.hasNext()
                    && result.size() < size) {

                result.add(
                        iterator.next()
                );
            }

            /*
             * Currently:
             *
             * Newest -> Oldest
             *
             * Reverse it so analytics receive:
             *
             * Oldest -> Newest
             */
            java.util.Collections.reverse(
                    result
            );

            return result;
        }
    }

    /**
     * Returns the number of cached candles.
     */
    public int getCandleCount(
            String instrumentKey,
            String timeframe) {

        NavigableMap<Instant, MarketCandle> candles =
                candleCache.get(
                        buildKey(
                                instrumentKey,
                                timeframe
                        )
                );

        if (candles == null) {
            return 0;
        }

        synchronized (candles) {

            return candles.size();
        }
    }

    /**
     * Returns whether the cache contains data for
     * the given instrument and timeframe.
     */
    public boolean contains(
            String instrumentKey,
            String timeframe) {

        NavigableMap<Instant, MarketCandle> candles =
                candleCache.get(
                        buildKey(
                                instrumentKey,
                                timeframe
                        )
                );

        return candles != null
                && !candles.isEmpty();
    }

    /**
     * Returns whether a specific candle exists in cache.
     */
    public boolean containsCandle(
            String instrumentKey,
            String timeframe,
            Instant timestamp) {

        if (timestamp == null) {
            return false;
        }

        NavigableMap<Instant, MarketCandle> candles =
                candleCache.get(
                        buildKey(
                                instrumentKey,
                                timeframe
                        )
                );

        if (candles == null) {
            return false;
        }

        synchronized (candles) {

            return candles.containsKey(
                    timestamp
            );
        }
    }

    /**
     * Removes all cached candles for a specific
     * instrument + timeframe.
     */
    public void clear(
            String instrumentKey,
            String timeframe) {

        candleCache.remove(
                buildKey(
                        instrumentKey,
                        timeframe
                )
        );
    }

    /**
     * Clears the complete market candle cache.
     */
    public void clearAll() {

        candleCache.clear();
    }

    /**
     * Creates the internal cache key.
     */
    private String buildKey(
            String instrumentKey,
            String timeframe) {

        return instrumentKey
                + "_"
                + timeframe;
    }
}