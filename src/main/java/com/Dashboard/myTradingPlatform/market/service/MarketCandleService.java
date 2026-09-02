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

    private final CandleAggregationService candleAggregationService;

    /*
     * =========================================================
     * CURRENT LIVE CANDLES
     * =========================================================
     */
    private final Map<String, MarketCandle> currentCandles =
            new ConcurrentHashMap<>();

    /*
     * =========================================================
     * CONSTRUCTOR
     * =========================================================
     */
    public MarketCandleService(
            MarketCandleCache marketCandleCache,
            MarketCandlePersistenceService persistenceService,
            CandleAggregationService candleAggregationService) {

        this.marketCandleCache =
                marketCandleCache;

        this.persistenceService =
                persistenceService;

        this.candleAggregationService =
                candleAggregationService;
    }

    /*
     * =========================================================
     * PROCESS LIVE CANDLE
     * =========================================================
     */
    public void processCandle(
            MarketCandle candle) {

        if (!isValid(candle)) {

            log.warn(
                    "Ignoring invalid live candle: {}",
                    candle
            );

            return;
        }

        String instrumentKey =
                candle.instrumentKey();

        String timeframe =
                candle.timeframe();

        String key =
                buildKey(
                        instrumentKey,
                        timeframe
                );

        log.debug(
                "Processing live candle: instrument={}, timeframe={}, close={}, timestamp={}",
                instrumentKey,
                timeframe,
                candle.close(),
                candle.timestamp()
        );

        /*
         * =====================================================
         * UPDATE MAIN CACHE
         * =====================================================
         */
        marketCandleCache.put(candle);

        log.debug(
                "Live candle cached: instrument={}, timeframe={}, timestamp={}",
                instrumentKey,
                timeframe,
                candle.timestamp()
        );

        /*
         * =====================================================
         * I1 -> I5 / I15 AGGREGATION
         * =====================================================
         */
        if ("I1".equalsIgnoreCase(timeframe)) {

            processI1Aggregation(
                    candle
            );
        }

        /*
         * =====================================================
         * CURRENT CANDLE TRACKING
         * =====================================================
         */
        MarketCandle previous =
                currentCandles.get(key);

        /*
         * =====================================================
         * FIRST CANDLE
         * =====================================================
         */
        if (previous == null) {

            currentCandles.put(
                    key,
                    candle
            );

            log.debug(
                    "Started tracking current candle: instrument={}, timeframe={}, timestamp={}",
                    instrumentKey,
                    timeframe,
                    candle.timestamp()
            );

            return;
        }

        /*
         * =====================================================
         * SAME CANDLE
         * =====================================================
         */
        if (previous.timestamp().equals(
                candle.timestamp())) {

            currentCandles.put(
                    key,
                    candle
            );

            log.debug(
                    "Updating current candle without DB persistence: instrument={}, timeframe={}, timestamp={}",
                    instrumentKey,
                    timeframe,
                    candle.timestamp()
            );

            return;
        }

        /*
         * =====================================================
         * NEWER CANDLE
         * =====================================================
         */
        if (candle.timestamp().isAfter(
                previous.timestamp())) {

            log.info(
                    "Candle completed: instrument={}, timeframe={}, timestamp={}",
                    previous.instrumentKey(),
                    previous.timeframe(),
                    previous.timestamp()
            );

            /*
             * Persist completed candle.
             */
            persistCompletedCandle(
                    previous
            );

            /*
             * Start tracking new candle.
             */
            currentCandles.put(
                    key,
                    candle
            );

            log.debug(
                    "Started tracking new candle: instrument={}, timeframe={}, timestamp={}",
                    instrumentKey,
                    timeframe,
                    candle.timestamp()
            );

            return;
        }

        /*
         * =====================================================
         * OLDER / OUT-OF-ORDER CANDLE
         * =====================================================
         */
        log.debug(
                "Ignoring out-of-order live candle: instrument={}, timeframe={}, timestamp={}, currentTimestamp={}",
                instrumentKey,
                timeframe,
                candle.timestamp(),
                previous.timestamp()
        );
    }

    /*
     * =========================================================
     * PROCESS I1 AGGREGATION
     * =========================================================
     */
    private void processI1Aggregation(
            MarketCandle i1Candle) {

        try {

            CandleAggregationService.AggregationResult result =
                    candleAggregationService.processI1Candle(
                            i1Candle
                    );

            String instrumentKey =
                    i1Candle.instrumentKey();

            /*
             * =====================================================
             * CURRENT I5
             * =====================================================
             */
            MarketCandle currentI5 =
                    candleAggregationService.getCurrentI5(
                            instrumentKey
                    );

            if (currentI5 != null) {

                marketCandleCache.put(
                        currentI5
                );

                log.debug(
                        "Current I5 candle cached: instrument={}, timestamp={}, close={}",
                        instrumentKey,
                        currentI5.timestamp(),
                        currentI5.close()
                );
            }

            /*
             * =====================================================
             * CURRENT I15
             * =====================================================
             */
            MarketCandle currentI15 =
                    candleAggregationService.getCurrentI15(
                            instrumentKey
                    );

            if (currentI15 != null) {

                marketCandleCache.put(
                        currentI15
                );

                log.debug(
                        "Current I15 candle cached: instrument={}, timestamp={}, close={}",
                        instrumentKey,
                        currentI15.timestamp(),
                        currentI15.close()
                );
            }

            /*
             * =====================================================
             * COMPLETED I5
             * =====================================================
             */
            if (result.completedI5() != null) {

                MarketCandle completedI5 =
                        result.completedI5();

                marketCandleCache.put(
                        completedI5
                );

                persistCompletedCandle(
                        completedI5
                );

                log.info(
                        "Completed I5 candle processed: instrument={}, timestamp={}, close={}",
                        completedI5.instrumentKey(),
                        completedI5.timestamp(),
                        completedI5.close()
                );
            }

            /*
             * =====================================================
             * COMPLETED I15
             * =====================================================
             */
            if (result.completedI15() != null) {

                MarketCandle completedI15 =
                        result.completedI15();

                marketCandleCache.put(
                        completedI15
                );

                persistCompletedCandle(
                        completedI15
                );

                log.info(
                        "Completed I15 candle processed: instrument={}, timestamp={}, close={}",
                        completedI15.instrumentKey(),
                        completedI15.timestamp(),
                        completedI15.close()
                );
            }

        } catch (Exception e) {

            log.error(
                    "Failed to process I1 aggregation: instrument={}, timestamp={}",
                    i1Candle.instrumentKey(),
                    i1Candle.timestamp(),
                    e
            );
        }
    }

    /*
     * =========================================================
     * GET CACHED CANDLE COUNT
     * =========================================================
     */
    public int getCachedCandleCount(
            String instrumentKey,
            String timeframe) {

        return marketCandleCache.getCandleCount(
                instrumentKey,
                timeframe
        );
    }

    /*
     * =========================================================
     * RESTORE HISTORICAL CANDLES INTO CACHE
     * =========================================================
     *
     * Used when historical candles already exist in DB.
     *
     * IMPORTANT:
     *
     * This method ONLY updates the in-memory cache.
     *
     * It does NOT:
     * - save to DB
     * - update currentCandles
     * - trigger I5/I15 aggregation
     *
     * Historical I5/I15 candles already come directly from
     * the historical API, so aggregation is not required.
     */
    public void cacheHistoricalCandles(
            List<MarketCandle> candles) {

        if (candles == null
                || candles.isEmpty()) {

            log.debug(
                    "No historical candles to restore into cache"
            );

            return;
        }

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
                "Historical candles restored into cache: received={}, cached={}",
                candles.size(),
                cachedCount
        );
    }

    /*
     * =========================================================
     * PROCESS HISTORICAL CANDLES
     * =========================================================
     *
     * Used when historical candles are downloaded from Upstox.
     *
     * Historical candles:
     *
     * 1. Added to cache
     * 2. Persisted to DB
     * 3. Never added to currentCandles
     * 4. Never sent through live aggregation
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
         * =====================================================
         * FILTER INVALID CANDLES
         * =====================================================
         */
        List<MarketCandle> validCandles =
                candles.stream()
                        .filter(this::isValid)
                        .toList();

        if (validCandles.isEmpty()) {

            log.warn(
                    "No valid historical candles found"
            );

            return;
        }

        /*
         * =====================================================
         * UPDATE CACHE
         * =====================================================
         */
        for (MarketCandle candle :
                validCandles) {

            marketCandleCache.put(
                    candle
            );
        }

        /*
         * =====================================================
         * PERSIST HISTORICAL DATA
         * =====================================================
         */
        persistenceService.saveAll(
                validCandles
        );

        log.info(
                "Historical candles processed successfully: received={}, cached={}",
                candles.size(),
                validCandles.size()
        );
    }

    /*
     * =========================================================
     * INITIALIZE CURRENT CANDLE
     * =========================================================
     */
    public void initializeCurrentCandle(
            MarketCandle candle) {

        if (!isValid(candle)) {

            log.warn(
                    "Cannot initialize invalid current candle"
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
     * GET CURRENT LIVE CANDLE
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
     * GET CURRENT I5
     * =========================================================
     */
    public MarketCandle getCurrentI5Candle(
            String instrumentKey) {

        return candleAggregationService.getCurrentI5(
                instrumentKey
        );
    }

    /*
     * =========================================================
     * GET CURRENT I15
     * =========================================================
     */
    public MarketCandle getCurrentI15Candle(
            String instrumentKey) {

        return candleAggregationService.getCurrentI15(
                instrumentKey
        );
    }

    /*
     * =========================================================
     * PERSIST COMPLETED CANDLE
     * =========================================================
     */
    private void persistCompletedCandle(
            MarketCandle candle) {

        if (candle == null) {
            return;
        }

        try {

            boolean persisted =
                    persistenceService.save(
                            candle
                    );

            if (persisted) {

                log.debug(
                        "Completed candle persisted: instrument={}, timeframe={}, timestamp={}",
                        candle.instrumentKey(),
                        candle.timeframe(),
                        candle.timestamp()
                );

            } else {

                log.debug(
                        "Completed candle already exists: instrument={}, timeframe={}, timestamp={}",
                        candle.instrumentKey(),
                        candle.timeframe(),
                        candle.timestamp()
                );
            }

        } catch (Exception e) {

            /*
             * DB failure should not stop live market-data
             * processing.
             */
            log.error(
                    "Failed to persist completed candle: instrument={}, timeframe={}, timestamp={}",
                    candle.instrumentKey(),
                    candle.timeframe(),
                    candle.timestamp(),
                    e
            );
        }
    }

    /*
     * =========================================================
     * VALIDATION
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

        return candle.open() != null
                && candle.high() != null
                && candle.low() != null
                && candle.close() != null;
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
}