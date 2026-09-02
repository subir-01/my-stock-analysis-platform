package com.Dashboard.myTradingPlatform.market.service;

import com.Dashboard.myTradingPlatform.market.client.UpstoxHistoricalDataClient;
import com.Dashboard.myTradingPlatform.market.mapper.UpstoxHistoricalCandleMapper;
import com.Dashboard.myTradingPlatform.market.model.MarketCandle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class HistoricalDataService {

    private final UpstoxHistoricalDataClient historicalDataClient;

    private final UpstoxHistoricalCandleMapper historicalCandleMapper;

    private final MarketCandleService marketCandleService;

    private final MarketCandlePersistenceService persistenceService;


    public HistoricalDataService(
            UpstoxHistoricalDataClient historicalDataClient,
            UpstoxHistoricalCandleMapper historicalCandleMapper,
            MarketCandleService marketCandleService,
            MarketCandlePersistenceService persistenceService) {

        this.historicalDataClient =
                historicalDataClient;

        this.historicalCandleMapper =
                historicalCandleMapper;

        this.marketCandleService =
                marketCandleService;

        this.persistenceService =
                persistenceService;
    }


    /*
     * =========================================================
     * FIND LATEST CANDLE
     * =========================================================
     */

    @Transactional(readOnly = true)
    public Optional<MarketCandle> findLatestCandle(
            String instrumentKey,
            String timeframe) {

        return persistenceService.findLatestCandle(
                instrumentKey,
                timeframe
        );
    }


    /*
     * =========================================================
     * CHECK HISTORICAL DATA
     * =========================================================
     */

    @Transactional(readOnly = true)
    public boolean hasHistoricalData(
            String instrumentKey,
            String timeframe) {

        return findLatestCandle(
                instrumentKey,
                timeframe
        ).isPresent();
    }


    /*
     * =========================================================
     * GET DATABASE CANDLE COUNT
     * =========================================================
     */

    @Transactional(readOnly = true)
    public long getCandleCount(
            String instrumentKey,
            String timeframe) {

        return persistenceService.countCandles(
                instrumentKey,
                timeframe
        );
    }


    /*
     * =========================================================
     * GET CACHE CANDLE COUNT
     * =========================================================
     */

    public int getCachedCandleCount(
            String instrumentKey,
            String timeframe) {

        return marketCandleService.getCachedCandleCount(
                instrumentKey,
                timeframe
        );
    }


    /*
     * =========================================================
     * LOAD HISTORICAL DATA
     * =========================================================
     */

    public void loadHistoricalData(
            String instrumentKey,
            String timeframe,
            String fromDate,
            String toDate) {

        validateRequest(
                instrumentKey,
                timeframe,
                fromDate,
                toDate
        );

        log.info(
                "Loading historical data: instrument={}, timeframe={}, from={}, to={}",
                instrumentKey,
                timeframe,
                fromDate,
                toDate
        );

        HistoricalRequest request =
                resolveHistoricalRequest(
                        timeframe
                );

        log.info(
                "Resolved historical request: instrument={}, timeframe={}, unit={}, interval={}",
                instrumentKey,
                timeframe,
                request.unit(),
                request.interval()
        );


        /*
         * =====================================================
         * FETCH
         * =====================================================
         *
         * Upstox expects:
         *
         * toDate
         * fromDate
         *
         * in this client implementation.
         */

        String response =
                historicalDataClient.getHistoricalCandles(
                        instrumentKey,
                        request.unit(),
                        request.interval(),
                        toDate,
                        fromDate
                );

        if (response == null
                || response.isBlank()) {

            log.warn(
                    "Empty historical API response: instrument={}, timeframe={}",
                    instrumentKey,
                    timeframe
            );

            return;
        }


        /*
         * =====================================================
         * MAP
         * =====================================================
         */

        List<MarketCandle> candles =
                historicalCandleMapper.toMarketCandles(
                        instrumentKey,
                        timeframe,
                        response
                );

        if (candles == null
                || candles.isEmpty()) {

            log.warn(
                    "No historical candles received: instrument={}, timeframe={}, from={}, to={}",
                    instrumentKey,
                    timeframe,
                    fromDate,
                    toDate
            );

            return;
        }


        log.info(
                "Historical chunk received: instrument={}, timeframe={}, from={}, to={}, count={}",
                instrumentKey,
                timeframe,
                fromDate,
                toDate,
                candles.size()
        );


        /*
         * =====================================================
         * SAVE + CACHE
         * =====================================================
         */

        marketCandleService.processHistoricalCandles(
                candles
        );

        log.info(
                "Historical chunk processed: instrument={}, timeframe={}, count={}",
                instrumentKey,
                timeframe,
                candles.size()
        );


        log.info(
                "Cache verification after historical load: instrument={}, timeframe={}, cacheCount={}",
                instrumentKey,
                timeframe,
                getCachedCandleCount(
                        instrumentKey,
                        timeframe
                )
        );


        log.info(
                "Historical data loading completed: instrument={}, timeframe={}",
                instrumentKey,
                timeframe
        );
    }


    /*
     * =========================================================
     * LOAD LATEST DATABASE CANDLES INTO CACHE
     * =========================================================
     *
     * Important:
     *
     * Database
     *     ↓
     * latest candles
     *     ↓
     * MarketCandleService
     *     ↓
     * MarketCandleCache
     *
     * The initializer expects the number of candles loaded
     * to be returned.
     */

    @Transactional(readOnly = true)
    public int loadLatestCandlesIntoCache(
            String instrumentKey,
            String timeframe,
            int count) {

        if (instrumentKey == null
                || instrumentKey.isBlank()) {

            log.warn(
                    "Cannot load cache. Instrument key is blank."
            );

            return 0;
        }

        if (timeframe == null
                || timeframe.isBlank()) {

            log.warn(
                    "Cannot load cache. Timeframe is blank: instrument={}",
                    instrumentKey
            );

            return 0;
        }

        if (count <= 0) {

            return 0;
        }


        List<MarketCandle> candles =
                persistenceService.findLatestCandles(
                        instrumentKey,
                        timeframe,
                        count
                );

        if (candles == null
                || candles.isEmpty()) {

            log.warn(
                    "No candles found in DB for cache loading: instrument={}, timeframe={}",
                    instrumentKey,
                    timeframe
            );

            return 0;
        }


        /*
         * findLatestCandles() already returns:
         *
         * oldest -> newest
         *
         * This is exactly what we want.
         */

        marketCandleService.processHistoricalCandles(
                candles
        );


        int cacheCount =
                getCachedCandleCount(
                        instrumentKey,
                        timeframe
                );

        log.info(
                "Loaded candles from DB into cache: instrument={}, timeframe={}, loaded={}, cacheCount={}",
                instrumentKey,
                timeframe,
                candles.size(),
                cacheCount
        );

        return candles.size();
    }


    /*
     * =========================================================
     * RESOLVE HISTORICAL REQUEST
     * =========================================================
     */

    private HistoricalRequest resolveHistoricalRequest(
            String timeframe) {

        if (timeframe == null
                || timeframe.isBlank()) {

            throw new IllegalArgumentException(
                    "Timeframe must not be null or blank"
            );
        }

        return switch (
                timeframe.toUpperCase()
                ) {

            case "I1" ->
                    new HistoricalRequest(
                            "minutes",
                            1
                    );

            case "I5" ->
                    new HistoricalRequest(
                            "minutes",
                            5
                    );

            case "I15" ->
                    new HistoricalRequest(
                            "minutes",
                            15
                    );

            case "1D",
                 "D1" ->
                    new HistoricalRequest(
                            "days",
                            1
                    );

            default ->
                    throw new IllegalArgumentException(
                            "Unsupported timeframe: "
                                    + timeframe
                                    + ". Supported timeframes: I1, I5, I15, 1d"
                    );
        };
    }


    /*
     * =========================================================
     * VALIDATION
     * =========================================================
     */

    private void validateRequest(
            String instrumentKey,
            String timeframe,
            String fromDate,
            String toDate) {

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

        if (fromDate == null
                || fromDate.isBlank()) {

            throw new IllegalArgumentException(
                    "From date must not be null or blank"
            );
        }

        if (toDate == null
                || toDate.isBlank()) {

            throw new IllegalArgumentException(
                    "To date must not be null or blank"
            );
        }
    }


    /*
     * =========================================================
     * LOAD TODAY'S INTRADAY I1 CANDLES
     * =========================================================
     *
     * Loads today's 1-minute candles directly from the
     * Upstox intraday API.
     *
     * Upstox
     *     ↓
     * mapper
     *     ↓
     * MarketCandle
     *     ↓
     * cache + DB
     *
     * This is intentionally separate from
     * loadHistoricalData(), because historical
     * initialization stops at yesterday.
     */

    public List<MarketCandle> loadTodayIntradayCandles(
            String instrumentKey) {

        if (instrumentKey == null
                || instrumentKey.isBlank()) {

            throw new IllegalArgumentException(
                    "Instrument key must not be null or blank"
            );
        }

        log.info(
                "Loading today's intraday I1 candles: instrument={}",
                instrumentKey
        );


        String response =
                historicalDataClient.getIntradayCandles(
                        instrumentKey,
                        1
                );

        if (response == null
                || response.isBlank()) {

            log.warn(
                    "No today's intraday response: instrument={}",
                    instrumentKey
            );

            return List.of();
        }


        List<MarketCandle> candles =
                historicalCandleMapper.toMarketCandles(
                        instrumentKey,
                        "I1",
                        response
                );

        if (candles == null
                || candles.isEmpty()) {

            log.warn(
                    "No today's I1 candles mapped: instrument={}",
                    instrumentKey
            );

            return List.of();
        }


        /*
         * Store today's I1 candles in DB and cache.
         *
         * processHistoricalCandles() is safe here because
         * these candles are retrieved through the historical/
         * intraday REST API rather than the live WebSocket.
         */

        marketCandleService.processHistoricalCandles(
                candles
        );


        log.info(
                "Today's intraday I1 candles loaded: instrument={}, count={}, first={}, last={}",
                instrumentKey,
                candles.size(),
                candles.get(0).timestamp(),
                candles.get(candles.size() - 1).timestamp()
        );

        return candles;
    }


    /*
     * =========================================================
     * INTERNAL REQUEST
     * =========================================================
     */

    private record HistoricalRequest(
            String unit,
            int interval) {
    }
}