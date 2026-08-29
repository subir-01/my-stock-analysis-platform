package com.Dashboard.myTradingPlatform.market.service;

import com.Dashboard.myTradingPlatform.market.cache.MarketCandleCache;
import com.Dashboard.myTradingPlatform.market.model.MarketCandle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class MarketCandleService {

    private final MarketCandleCache marketCandleCache;
    private final MarketCandlePersistenceService persistenceService;

    /*
     * Stores the currently forming candle for:
     *
     * instrument + timeframe
     *
     * Example:
     *
     * NSE_EQ|INE002A01018_I1
     * GLOBAL_INDEX|SGX NIFTY_I1
     */
    private final Map<String, MarketCandle> currentCandles =
            new ConcurrentHashMap<>();

    public MarketCandleService(
            MarketCandleCache marketCandleCache,
            MarketCandlePersistenceService persistenceService) {

        this.marketCandleCache = marketCandleCache;
        this.persistenceService = persistenceService;
    }

    /*
     * =========================================================
     * PROCESS LIVE CANDLE
     * =========================================================
     *
     * This method is called by the WebSocket.
     *
     * Flow:
     *
     * Live candle
     *      ↓
     * Cache update
     *      ↓
     * Compare timestamp
     *      ↓
     * Same timestamp
     *      → update current candle only
     *
     * New timestamp
     *      → previous candle completed
     *      → persist previous candle
     *      → start tracking new candle
     *
     * Older timestamp
     *      → ignore
     */
    public void processCandle(
            MarketCandle candle) {

        if (candle == null) {

            log.warn(
                    "Received null live candle"
            );

            return;
        }

        if (!isValid(candle)) {

            log.warn(
                    "Received invalid live candle: {}",
                    candle
            );

            return;
        }

        String key =
                buildKey(
                        candle.instrumentKey(),
                        candle.timeframe()
                );

        log.debug(
                "Processing live candle: instrument={}, timeframe={}, close={}, timestamp={}",
                candle.instrumentKey(),
                candle.timeframe(),
                candle.close(),
                candle.timestamp()
        );

        /*
         * =====================================================
         * UPDATE CACHE
         * =====================================================
         *
         * The cache always contains the latest version of
         * the currently forming candle.
         */
        marketCandleCache.put(
                candle
        );

        log.debug(
                "Live candle cached: instrument={}, timeframe={}, timestamp={}",
                candle.instrumentKey(),
                candle.timeframe(),
                candle.timestamp()
        );

        /*
         * =====================================================
         * GET CURRENT CANDLE
         * =====================================================
         */
        MarketCandle previous =
                currentCandles.get(
                        key
                );

        /*
         * =====================================================
         * FIRST CANDLE
         * =====================================================
         *
         * No tracker exists yet.
         */
        if (previous == null) {

            currentCandles.put(
                    key,
                    candle
            );

            log.debug(
                    "Started tracking current candle: instrument={}, timeframe={}, timestamp={}",
                    candle.instrumentKey(),
                    candle.timeframe(),
                    candle.timestamp()
            );

            return;
        }

        /*
         * =====================================================
         * SAME CANDLE
         * =====================================================
         *
         * Upstox can repeatedly send updates for the same
         * candle timestamp.
         *
         * Example:
         *
         * 09:59 candle
         * 09:59 candle
         * 09:59 candle
         *
         * Do NOT write these updates to DB.
         */
        if (candle.timestamp().equals(
                previous.timestamp()
        )) {

            currentCandles.put(
                    key,
                    candle
            );

            log.debug(
                    "Updating current candle without DB persistence: instrument={}, timeframe={}, timestamp={}",
                    candle.instrumentKey(),
                    candle.timeframe(),
                    candle.timestamp()
            );

            return;
        }

        /*
         * =====================================================
         * NEWER CANDLE
         * =====================================================
         *
         * A newer timestamp means the previous candle has
         * completed.
         */
        if (candle.timestamp().isAfter(
                previous.timestamp()
        )) {

            log.info(
                    "Candle completed: instrument={}, timeframe={}, timestamp={}",
                    previous.instrumentKey(),
                    previous.timeframe(),
                    previous.timestamp()
            );

            /*
             * Persist completed candle.
             *
             * save() returns:
             *
             * true  -> inserted
             * false -> already exists
             */
            boolean persisted =
                    persistenceService.save(
                            previous
                    );

            if (persisted) {

                log.debug(
                        "Completed candle persisted: instrument={}, timeframe={}, timestamp={}",
                        previous.instrumentKey(),
                        previous.timeframe(),
                        previous.timestamp()
                );

            } else {

                log.debug(
                        "Completed candle already exists: instrument={}, timeframe={}, timestamp={}",
                        previous.instrumentKey(),
                        previous.timeframe(),
                        previous.timestamp()
                );
            }

            /*
             * =================================================
             * START NEW CURRENT CANDLE
             * =================================================
             */
            currentCandles.put(
                    key,
                    candle
            );

            log.debug(
                    "Started new candle: instrument={}, timeframe={}, timestamp={}",
                    candle.instrumentKey(),
                    candle.timeframe(),
                    candle.timestamp()
            );

            return;
        }

        /*
         * =====================================================
         * OLDER / OUT-OF-ORDER CANDLE
         * =====================================================
         *
         * Do not change currentCandles.
         */
        log.debug(
                "Ignoring out-of-order candle: instrument={}, timeframe={}, timestamp={}, currentTimestamp={}",
                candle.instrumentKey(),
                candle.timeframe(),
                candle.timestamp(),
                previous.timestamp()
        );
    }

    /*
     * =========================================================
     * PROCESS HISTORICAL CANDLES
     * =========================================================
     *
     * Historical candles must NOT affect currentCandles.
     *
     * They are:
     *
     * 1. Stored in DB if not already present
     * 2. Added to cache
     *
     * They do NOT become live/current candles.
     */
    public void processHistoricalCandles(
            List<MarketCandle> candles) {

        if (candles == null
                || candles.isEmpty()) {

            log.debug(
                    "No historical candles to process"
            );

            return;
        }

        log.info(
                "Processing historical candles: count={}",
                candles.size()
        );

        /*
         * Persist historical candles.
         */
        persistenceService.saveAll(
                candles
        );

        /*
         * Load them into cache.
         */
        int cachedCount = 0;

        for (MarketCandle candle : candles) {

            if (!isValid(candle)) {
                continue;
            }

            marketCandleCache.put(
                    candle
            );

            cachedCount++;
        }

        log.info(
                "Historical candles processed successfully: received={}, cached={}",
                candles.size(),
                cachedCount
        );
    }

    /*
     * =========================================================
     * INITIALIZE CURRENT CANDLE
     * =========================================================
     *
     * Called by MarketCandleCacheLoader after loading historical
     * candles from DB.
     *
     * IMPORTANT:
     *
     * This only initializes the tracker.
     *
     * It does NOT persist anything.
     */
    public void initializeCurrentCandle(
            MarketCandle candle) {

        if (!isValid(candle)) {

            log.warn(
                    "Cannot initialize invalid current candle: {}",
                    candle
            );

            return;
        }

        String key =
                buildKey(
                        candle.instrumentKey(),
                        candle.timeframe()
                );

        currentCandles.put(
                key,
                candle
        );

        log.debug(
                "Current candle initialized: instrument={}, timeframe={}, timestamp={}",
                candle.instrumentKey(),
                candle.timeframe(),
                candle.timestamp()
        );
    }

    /*
     * =========================================================
     * GET CURRENT CANDLE
     * =========================================================
     */
    public MarketCandle getCurrentCandle(
            String instrumentKey,
            String timeframe) {

        if (instrumentKey == null
                || timeframe == null) {

            return null;
        }

        return currentCandles.get(
                buildKey(
                        instrumentKey,
                        timeframe
                )
        );
    }

    /*
     * =========================================================
     * REMOVE CURRENT CANDLE
     * =========================================================
     */
    public void removeCurrentCandle(
            String instrumentKey,
            String timeframe) {

        if (instrumentKey == null
                || timeframe == null) {

            return;
        }

        currentCandles.remove(
                buildKey(
                        instrumentKey,
                        timeframe
                )
        );
    }

    /*
     * =========================================================
     * GET CURRENT CANDLE COUNT
     * =========================================================
     */
    public int getCurrentCandleCount() {

        return currentCandles.size();
    }

    /*
     * =========================================================
     * BUILD KEY
     * =========================================================
     */
    private String buildKey(
            String instrumentKey,
            String timeframe) {

        return instrumentKey
                + "_"
                + timeframe;
    }

    /*
     * =========================================================
     * VALIDATE CANDLE
     * =========================================================
     */
    private boolean isValid(
            MarketCandle candle) {

        if (candle == null) {
            return false;
        }

        if (candle.instrumentKey() == null
                || candle.instrumentKey().isBlank()) {
            return false;
        }

        if (candle.timeframe() == null
                || candle.timeframe().isBlank()) {
            return false;
        }

        if (candle.timestamp() == null) {
            return false;
        }

        if (candle.open() == null
                || candle.high() == null
                || candle.low() == null
                || candle.close() == null) {
            return false;
        }

        return true;
    }
}