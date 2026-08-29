package com.Dashboard.myTradingPlatform.market.service;

import com.Dashboard.myTradingPlatform.market.client.UpstoxHistoricalDataClient;
import com.Dashboard.myTradingPlatform.market.mapper.UpstoxHistoricalCandleMapper;
import com.Dashboard.myTradingPlatform.market.model.MarketCandle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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

        this.historicalDataClient = historicalDataClient;
        this.historicalCandleMapper = historicalCandleMapper;
        this.marketCandleService = marketCandleService;
        this.persistenceService = persistenceService;
    }

    public Optional<MarketCandle> findLatestCandle(
            String instrumentKey,
            String timeframe) {

        return persistenceService.findLatestCandle(
                instrumentKey,
                timeframe
        );
    }

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
     * LOAD HISTORICAL DATA
     * =========================================================
     */
    public void loadHistoricalData(
            String instrumentKey,
            String timeframe,
            String fromDate,
            String toDate) {

        log.info(
                "Loading historical data: instrument={}, timeframe={}, from={}, to={}",
                instrumentKey,
                timeframe,
                fromDate,
                toDate
        );

        HistoricalRequest request =
                resolveHistoricalRequest(timeframe);

        LocalDate from =
                LocalDate.parse(fromDate);

        LocalDate to =
                LocalDate.parse(toDate);

        /*
         * Process the requested range in chunks.
         */
        LocalDate chunkStart = from;

        while (!chunkStart.isAfter(to)) {

            LocalDate chunkEnd =
                    calculateChunkEnd(
                            chunkStart,
                            to,
                            request.chunkDays()
                    );

            processChunk(
                    instrumentKey,
                    timeframe,
                    request,
                    chunkStart,
                    chunkEnd
            );

            /*
             * Move to the next chunk.
             */
            chunkStart =
                    chunkEnd.plusDays(1);
        }

        log.info(
                "Historical data loading completed: instrument={}, timeframe={}",
                instrumentKey,
                timeframe
        );
    }

    /*
     * =========================================================
     * PROCESS ONE CHUNK
     * =========================================================
     */
    private void processChunk(
            String instrumentKey,
            String timeframe,
            HistoricalRequest request,
            LocalDate fromDate,
            LocalDate toDate) {

        log.info(
                "Fetching historical chunk: instrument={}, timeframe={}, from={}, to={}",
                instrumentKey,
                timeframe,
                fromDate,
                toDate
        );

        try {

            String response =
                    historicalDataClient.getHistoricalCandles(
                            instrumentKey,
                            request.unit(),
                            request.interval(),
                            toDate.toString(),
                            fromDate.toString()
                    );

            List<MarketCandle> candles =
                    historicalCandleMapper.toMarketCandles(
                            instrumentKey,
                            timeframe,
                            response
                    );

            if (candles == null || candles.isEmpty()) {

                log.info(
                        "No candles received for chunk: instrument={}, timeframe={}, from={}, to={}",
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
             * Historical candles go through the historical
             * processing path.
             */
            marketCandleService.processHistoricalCandles(
                    candles
            );

            log.info(
                    "Historical chunk processed: instrument={}, timeframe={}, from={}, to={}, count={}",
                    instrumentKey,
                    timeframe,
                    fromDate,
                    toDate,
                    candles.size()
            );

        } catch (Exception e) {

            /*
             * Don't stop the complete historical loading
             * process because one chunk failed.
             */
            log.error(
                    "Failed to process historical chunk: instrument={}, timeframe={}, from={}, to={}",
                    instrumentKey,
                    timeframe,
                    fromDate,
                    toDate,
                    e
            );
        }
    }

    /*
     * =========================================================
     * CALCULATE CHUNK END
     * =========================================================
     */
    private LocalDate calculateChunkEnd(
            LocalDate chunkStart,
            LocalDate requestedEnd,
            int chunkDays) {

        LocalDate chunkEnd =
                chunkStart.plusDays(
                        chunkDays - 1
                );

        if (chunkEnd.isAfter(requestedEnd)) {
            return requestedEnd;
        }

        return chunkEnd;
    }

    /*
     * =========================================================
     * RESOLVE TIMEFRAME
     * =========================================================
     */
    private HistoricalRequest resolveHistoricalRequest(
            String timeframe) {

        if (timeframe == null || timeframe.isBlank()) {

            throw new IllegalArgumentException(
                    "Timeframe must not be null or blank"
            );
        }

        return switch (timeframe.toUpperCase()) {

            case "I1" ->
                    new HistoricalRequest(
                            "minutes",
                            1,
                            30
                    );

            case "I5" ->
                    new HistoricalRequest(
                            "minutes",
                            5,
                            30
                    );

            case "I15" ->
                    new HistoricalRequest(
                            "minutes",
                            15,
                            30
                    );

            case "D1", "1D" ->
                    new HistoricalRequest(
                            "days",
                            1,
                            365
                    );

            default ->
                    throw new IllegalArgumentException(
                            "Unsupported timeframe: "
                                    + timeframe
                                    + ". Supported: I1, I5, I15, D1"
                    );
        };
    }

    /*
     * =========================================================
     * INTERNAL REQUEST
     * =========================================================
     */
    private record HistoricalRequest(
            String unit,
            int interval,
            int chunkDays) {
    }
}