package com.Dashboard.myTradingPlatform.market.initializer;

import com.Dashboard.myTradingPlatform.market.analytics.model.MarketInstrument;
import com.Dashboard.myTradingPlatform.market.model.MarketCandle;
import com.Dashboard.myTradingPlatform.market.service.CandleAggregationService;
import com.Dashboard.myTradingPlatform.market.service.HistoricalDataService;
import com.Dashboard.myTradingPlatform.market.service.MarketInstrumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@Slf4j
public class HistoricalDataInitializer {

    private final HistoricalDataService historicalDataService;

    private final MarketInstrumentService marketInstrumentService;

    private final MarketDataInitializationState initializationState;

    private final CandleAggregationService candleAggregationService;


    /*
     * =========================================================
     * MINIMUM DATABASE CANDLES
     * =========================================================
     *
     * EMA50 requires at least 50 candles.
     *
     * We keep a larger database history so that future
     * analytics can be added without downloading again.
     */

    private static final long MIN_INTRADAY_CANDLES = 500;

    private static final long MIN_DAILY_CANDLES = 100;


    /*
     * =========================================================
     * CACHE SIZE
     * =========================================================
     *
     * MarketAnalyticsService currently analyzes 100 candles.
     *
     * Therefore we restore the latest 200 candles into memory.
     */

    private static final int CACHE_CANDLE_COUNT = 200;


    /*
     * =========================================================
     * HISTORICAL LOOKBACK
     * =========================================================
     */

    private static final int INTRADAY_LOOKBACK_DAYS = 30;

    private static final int DAILY_LOOKBACK_DAYS = 180;


    /*
     * =========================================================
     * REQUIRED TIMEFRAMES
     * =========================================================
     */

    private static final List<String> TIMEFRAMES =
            List.of(
                    "I1",
                    "I5",
                    "I15",
                    "1d"
            );


    /*
     * =========================================================
     * CONSTRUCTOR
     * =========================================================
     */

    public HistoricalDataInitializer(
            HistoricalDataService historicalDataService,
            MarketInstrumentService marketInstrumentService,
            MarketDataInitializationState initializationState,
            CandleAggregationService candleAggregationService) {

        this.historicalDataService =
                historicalDataService;

        this.marketInstrumentService =
                marketInstrumentService;

        this.initializationState =
                initializationState;

        this.candleAggregationService =
                candleAggregationService;
    }


    /*
     * =========================================================
     * APPLICATION READY
     * =========================================================
     */

    @EventListener(ApplicationReadyEvent.class)
    public void loadHistoricalData() {

        initializationState.reset();

        log.info(
                "========================================================="
        );

        log.info(
                "Starting historical market data initialization"
        );

        log.info(
                "========================================================="
        );


        List<MarketInstrument> instruments =
                marketInstrumentService.getEnabledInstruments();

        if (instruments == null
                || instruments.isEmpty()) {

            log.warn(
                    "No enabled market instruments found"
            );

            return;
        }


        log.info(
                "Enabled instruments found: {}",
                instruments.size()
        );


        boolean allSuccessful = true;


        /*
         * =====================================================
         * PROCESS ALL INSTRUMENTS
         * =====================================================
         */

        for (MarketInstrument instrument :
                instruments) {

            if (instrument == null) {
                continue;
            }


            String instrumentKey =
                    instrument.instrumentKey();

            if (instrumentKey == null
                    || instrumentKey.isBlank()) {

                log.warn(
                        "Skipping instrument because instrument key is blank"
                );

                allSuccessful = false;

                continue;
            }


            log.info(
                    "Initializing instrument: {}",
                    instrumentKey
            );


            /*
             * =================================================
             * PROCESS HISTORICAL TIMEFRAMES
             * =================================================
             */

            for (String timeframe : TIMEFRAMES) {

                boolean success =
                        loadTimeframe(
                                instrumentKey,
                                timeframe
                        );

                if (!success) {

                    allSuccessful = false;
                }
            }


            /*
             * =================================================
             * LOAD TODAY'S I1 DATA
             * =================================================
             *
             * Historical data intentionally stops at yesterday.
             *
             * Therefore we separately load today's I1 candles
             * and use them to reconstruct the current I5/I15
             * aggregation buckets.
             */

            initializeTodayIntradayData(
                    instrumentKey
            );
        }


        /*
         * =====================================================
         * FINAL STATUS
         * =====================================================
         */

        if (allSuccessful) {

            initializationState.markInitialized();

            log.info(
                    "========================================================="
            );

            log.info(
                    "Historical market data initialization completed successfully"
            );

            log.info(
                    "Today's intraday aggregation state initialized"
            );

            log.info(
                    "Analytics is READY"
            );

            log.info(
                    "========================================================="
            );

        } else {

            log.warn(
                    "========================================================="
            );

            log.warn(
                    "Historical market data initialization completed with errors"
            );

            log.warn(
                    "Today's intraday aggregation was attempted"
            );

            log.warn(
                    "Analytics is NOT fully READY"
            );

            log.warn(
                    "========================================================="
            );
        }
    }


    /*
     * =========================================================
     * LOAD ONE TIMEFRAME
     * =========================================================
     */

    private boolean loadTimeframe(
            String instrumentKey,
            String timeframe) {

        try {

            /*
             * =================================================
             * REQUIRED DATABASE CANDLE COUNT
             * =================================================
             */

            long minimumRequired =
                    getMinimumRequiredCandles(
                            timeframe
                    );


            /*
             * =================================================
             * CURRENT DATABASE COUNT
             * =================================================
             */

            long existingCount =
                    historicalDataService.getCandleCount(
                            instrumentKey,
                            timeframe
                    );


            /*
             * =================================================
             * CURRENT CACHE COUNT
             * =================================================
             */

            int existingCacheCount =
                    historicalDataService.getCachedCandleCount(
                            instrumentKey,
                            timeframe
                    );


            log.info(
                    "Historical data status: instrument={}, timeframe={}, dbCandles={}, cacheCandles={}, requiredDbCandles={}",
                    instrumentKey,
                    timeframe,
                    existingCount,
                    existingCacheCount,
                    minimumRequired
            );


            /*
             * =================================================
             * DATABASE ALREADY HAS ENOUGH DATA
             * =================================================
             */

            if (existingCount >= minimumRequired) {

                log.info(
                        "Sufficient historical data exists: instrument={}, timeframe={}, dbCandles={}",
                        instrumentKey,
                        timeframe,
                        existingCount
                );


                /*
                 * =================================================
                 * RESTORE DB -> CACHE
                 * =================================================
                 *
                 * Even if DB has thousands of candles, analytics
                 * cannot use them unless the latest candles are
                 * available in MarketCandleCache.
                 */

                int loaded =
                        historicalDataService.loadLatestCandlesIntoCache(
                                instrumentKey,
                                timeframe,
                                CACHE_CANDLE_COUNT
                        );


                int finalCacheCount =
                        historicalDataService.getCachedCandleCount(
                                instrumentKey,
                                timeframe
                        );


                log.info(
                        "Historical cache restoration completed: instrument={}, timeframe={}, loaded={}, cacheCandles={}",
                        instrumentKey,
                        timeframe,
                        loaded,
                        finalCacheCount
                );


                /*
                 * We only need enough candles in cache for
                 * analytics.
                 */

                if (finalCacheCount < CACHE_CANDLE_COUNT) {

                    log.warn(
                            "Cache contains fewer candles than requested: instrument={}, timeframe={}, cacheCandles={}, requested={}",
                            instrumentKey,
                            timeframe,
                            finalCacheCount,
                            CACHE_CANDLE_COUNT
                    );
                }


                /*
                 * EMA50 requires at least 50 candles.
                 */

                if (finalCacheCount < 50) {

                    log.error(
                            "Historical data exists in DB but cache has insufficient candles: instrument={}, timeframe={}, cacheCandles={}",
                            instrumentKey,
                            timeframe,
                            finalCacheCount
                    );

                    return false;
                }


                return true;
            }


            /*
             * =================================================
             * DATABASE DOES NOT HAVE ENOUGH DATA
             * =================================================
             */

            log.info(
                    "Insufficient historical data. Downloading more: instrument={}, timeframe={}, existing={}, required={}",
                    instrumentKey,
                    timeframe,
                    existingCount,
                    minimumRequired
            );


            /*
             * =================================================
             * DETERMINE LOOKBACK
             * =================================================
             */

            int lookbackDays =
                    getLookbackDays(
                            timeframe
                    );


            /*
             * We download until yesterday.
             *
             * This avoids downloading an incomplete current
             * trading day through the historical API.
             */

            LocalDate toDate =
                    LocalDate.now()
                            .minusDays(1);

            LocalDate fromDate =
                    toDate.minusDays(
                            lookbackDays
                    );


            log.info(
                    "Historical download date range: instrument={}, timeframe={}, from={}, to={}",
                    instrumentKey,
                    timeframe,
                    fromDate,
                    toDate
            );


            /*
             * =================================================
             * DOWNLOAD HISTORICAL DATA
             * =================================================
             */

            historicalDataService.loadHistoricalData(
                    instrumentKey,
                    timeframe,
                    fromDate.toString(),
                    toDate.toString()
            );


            /*
             * =================================================
             * VERIFY DATABASE
             * =================================================
             */

            long finalDbCount =
                    historicalDataService.getCandleCount(
                            instrumentKey,
                            timeframe
                    );


            log.info(
                    "Historical DB verification: instrument={}, timeframe={}, candlesAfterDownload={}, required={}",
                    instrumentKey,
                    timeframe,
                    finalDbCount,
                    minimumRequired
            );


            if (finalDbCount < minimumRequired) {

                log.error(
                        "Historical data still insufficient after download: instrument={}, timeframe={}, candles={}, required={}",
                        instrumentKey,
                        timeframe,
                        finalDbCount,
                        minimumRequired
                );

                return false;
            }


            /*
             * =================================================
             * EXPLICIT DB -> CACHE RESTORATION
             * =================================================
             *
             * Do this even if loadHistoricalData() already
             * populated the cache.
             *
             * This guarantees that the latest 200 DB candles
             * are available to MarketAnalyticsService.
             */

            int loaded =
                    historicalDataService.loadLatestCandlesIntoCache(
                            instrumentKey,
                            timeframe,
                            CACHE_CANDLE_COUNT
                    );


            /*
             * =================================================
             * VERIFY CACHE
             * =================================================
             */

            int finalCacheCount =
                    historicalDataService.getCachedCandleCount(
                            instrumentKey,
                            timeframe
                    );


            log.info(
                    "Historical cache verification: instrument={}, timeframe={}, loaded={}, cacheCandles={}, requiredForAnalytics=50",
                    instrumentKey,
                    timeframe,
                    loaded,
                    finalCacheCount
            );


            /*
             * EMA50 requires at least 50 candles.
             */

            if (finalCacheCount < 50) {

                log.error(
                        "Historical data exists in DB but cache is insufficient for analytics: instrument={}, timeframe={}, cacheCandles={}",
                        instrumentKey,
                        timeframe,
                        finalCacheCount
                );

                return false;
            }


            /*
             * =================================================
             * SUCCESS
             * =================================================
             */

            log.info(
                    "Historical data initialized successfully: instrument={}, timeframe={}, dbCandles={}, cacheCandles={}",
                    instrumentKey,
                    timeframe,
                    finalDbCount,
                    finalCacheCount
            );

            return true;


        } catch (Exception e) {

            log.error(
                    "Failed to initialize historical data: instrument={}, timeframe={}",
                    instrumentKey,
                    timeframe,
                    e
            );

            return false;
        }
    }


    /*
     * =========================================================
     * MINIMUM DATABASE CANDLES
     * =========================================================
     */

    private long getMinimumRequiredCandles(
            String timeframe) {

        if (timeframe == null
                || timeframe.isBlank()) {

            return MIN_INTRADAY_CANDLES;
        }

        return switch (
                timeframe.toUpperCase()
                ) {

            case "1D",
                 "D1" ->
                    MIN_DAILY_CANDLES;

            case "I1",
                 "I5",
                 "I15" ->
                    MIN_INTRADAY_CANDLES;

            default ->
                    MIN_INTRADAY_CANDLES;
        };
    }


    /*
     * =========================================================
     * HISTORICAL LOOKBACK DAYS
     * =========================================================
     */

    private int getLookbackDays(
            String timeframe) {

        if (timeframe == null
                || timeframe.isBlank()) {

            return INTRADAY_LOOKBACK_DAYS;
        }

        return switch (
                timeframe.toUpperCase()
                ) {

            case "1D",
                 "D1" ->
                    DAILY_LOOKBACK_DAYS;

            case "I1",
                 "I5",
                 "I15" ->
                    INTRADAY_LOOKBACK_DAYS;

            default ->
                    INTRADAY_LOOKBACK_DAYS;
        };
    }


    /*
     * =========================================================
     * INITIALIZE TODAY'S INTRADAY DATA
     * =========================================================
     *
     * Historical initialization stops at yesterday.
     *
     * Today's I1 candles are loaded separately from the
     * Upstox intraday endpoint.
     *
     * Those candles are then passed through the exact same
     * CandleAggregationService used by the live WebSocket.
     *
     * This allows the aggregation service to reconstruct
     * the current I5 and I15 buckets before live data arrives.
     */

    private void initializeTodayIntradayData(
            String instrumentKey) {

        try {

            log.info(
                    "========================================================="
            );

            log.info(
                    "Loading today's intraday I1 data: instrument={}",
                    instrumentKey
            );

            log.info(
                    "========================================================="
            );


            List<MarketCandle> todayCandles =
                    historicalDataService.loadTodayIntradayCandles(
                            instrumentKey
                    );


            if (todayCandles == null
                    || todayCandles.isEmpty()) {

                log.warn(
                        "No today's intraday candles available: instrument={}",
                        instrumentKey
                );

                return;
            }


            /*
             * =================================================
             * FEED TODAY'S I1 CANDLES INTO AGGREGATION
             * =================================================
             *
             * Process candles in chronological order.
             *
             * CandleAggregationService internally keeps the
             * I5 and I15 buckets for this instrument.
             */

            for (MarketCandle candle :
                    todayCandles) {

                if (candle == null) {
                    continue;
                }

                candleAggregationService.processI1Candle(
                        candle
                );
            }


            log.info(
                    "Today's I1 aggregation state initialized: instrument={}, candles={}",
                    instrumentKey,
                    todayCandles.size()
            );


            /*
             * =================================================
             * CURRENT BUCKET COUNTS
             * =================================================
             *
             * These logs help us verify that the current I5
             * and I15 buckets have actually been reconstructed.
             */

            int currentI5Count =
                    candleAggregationService.getCurrentI5CandleCount(
                            instrumentKey
                    );

            int currentI15Count =
                    candleAggregationService.getCurrentI15CandleCount(
                            instrumentKey
                    );


            MarketCandle currentI5 =
                    candleAggregationService.getCurrentI5(
                            instrumentKey
                    );

            MarketCandle currentI15 =
                    candleAggregationService.getCurrentI15(
                            instrumentKey
                    );


            log.info(
                    "Today's I5 aggregation state: instrument={}, I1Count={}, timestamp={}, volume={}",
                    instrumentKey,
                    currentI5Count,
                    currentI5 != null
                            ? currentI5.timestamp()
                            : null,
                    currentI5 != null
                            ? currentI5.volume()
                            : null
            );


            log.info(
                    "Today's I15 aggregation state: instrument={}, I1Count={}, timestamp={}, volume={}",
                    instrumentKey,
                    currentI15Count,
                    currentI15 != null
                            ? currentI15.timestamp()
                            : null,
                    currentI15 != null
                            ? currentI15.volume()
                            : null
            );


        } catch (Exception e) {

            log.error(
                    "Failed to initialize today's intraday data: instrument={}",
                    instrumentKey,
                    e
            );
        }
    }
}