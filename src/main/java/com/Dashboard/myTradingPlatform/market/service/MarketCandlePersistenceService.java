package com.Dashboard.myTradingPlatform.market.service;

import com.Dashboard.myTradingPlatform.market.event.MarketCandleEntity;
import com.Dashboard.myTradingPlatform.market.model.MarketCandle;
import com.Dashboard.myTradingPlatform.market.repository.MarketCandleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class MarketCandlePersistenceService {

    private static final int MAX_CACHE_LOAD = 500;

    private final MarketCandleRepository repository;

    public MarketCandlePersistenceService(
            MarketCandleRepository repository) {

        this.repository = repository;
    }

    /*
     * =========================================================
     * SAVE SINGLE CANDLE
     * =========================================================
     *
     * Used primarily for completed live candles.
     *
     * Returns:
     *
     * true  -> candle inserted
     * false -> candle already exists
     */
    @Transactional
    public boolean save(MarketCandle candle) {

        if (candle == null) {

            log.warn(
                    "Cannot persist null candle"
            );

            return false;
        }

        if (!isValid(candle)) {

            log.warn(
                    "Cannot persist invalid candle: {}",
                    candle
            );

            return false;
        }

        boolean exists =
                repository
                        .existsByInstrumentKeyAndTimeframeAndTimestamp(
                                candle.instrumentKey(),
                                candle.timeframe(),
                                candle.timestamp()
                        );

        if (exists) {

            log.debug(
                    "Candle already exists: instrument={}, timeframe={}, timestamp={}",
                    candle.instrumentKey(),
                    candle.timeframe(),
                    candle.timestamp()
            );

            return false;
        }

        MarketCandleEntity entity =
                toEntity(candle);

        repository.save(entity);

        log.debug(
                "Candle inserted: instrument={}, timeframe={}, timestamp={}",
                candle.instrumentKey(),
                candle.timeframe(),
                candle.timestamp()
        );

        return true;
    }

    /*
     * =========================================================
     * SAVE HISTORICAL CANDLES
     * =========================================================
     *
     * Historical API may return thousands of candles.
     *
     * Existing candles are skipped.
     */
    @Transactional
    public void saveAll(
            List<MarketCandle> candles) {

        if (candles == null
                || candles.isEmpty()) {

            log.debug(
                    "No historical candles to persist"
            );

            return;
        }

        int savedCount = 0;
        int duplicateCount = 0;
        int invalidCount = 0;

        for (MarketCandle candle : candles) {

            if (!isValid(candle)) {

                invalidCount++;

                continue;
            }

            boolean exists =
                    repository
                            .existsByInstrumentKeyAndTimeframeAndTimestamp(
                                    candle.instrumentKey(),
                                    candle.timeframe(),
                                    candle.timestamp()
                            );

            if (exists) {

                duplicateCount++;

                continue;
            }

            repository.save(
                    toEntity(candle)
            );

            savedCount++;
        }

        log.info(
                "Historical candles processed: received={}, newlySaved={}, duplicates={}, invalid={}",
                candles.size(),
                savedCount,
                duplicateCount,
                invalidCount
        );
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

        if (instrumentKey == null
                || instrumentKey.isBlank()
                || timeframe == null
                || timeframe.isBlank()) {

            return Optional.empty();
        }

        return repository
                .findTopByInstrumentKeyAndTimeframeOrderByTimestampDesc(
                        instrumentKey,
                        timeframe
                )
                .map(this::toMarketCandle);
    }

    /*
     * =========================================================
     * FIND LATEST N CANDLES
     * =========================================================
     *
     * Database returns:
     *
     * newest -> oldest
     *
     * We reverse it before returning:
     *
     * oldest -> newest
     *
     * This is important for:
     *
     * SMA
     * EMA
     * RSI
     * Support
     * Resistance
     */
    @Transactional(readOnly = true)
    public List<MarketCandle> findLatestCandles(
            String instrumentKey,
            String timeframe,
            int limit) {

        if (instrumentKey == null
                || instrumentKey.isBlank()
                || timeframe == null
                || timeframe.isBlank()
                || limit <= 0) {

            return List.of();
        }

        int actualLimit =
                Math.min(
                        limit,
                        MAX_CACHE_LOAD
                );

        List<MarketCandleEntity> entities =
                repository
                        .findTop500ByInstrumentKeyAndTimeframeOrderByTimestampDesc(
                                instrumentKey,
                                timeframe
                        );

        if (entities == null
                || entities.isEmpty()) {

            return List.of();
        }

        List<MarketCandle> candles =
                entities.stream()
                        .limit(actualLimit)
                        .map(this::toMarketCandle)
                        .toList();

        /*
         * DB order:
         *
         * newest -> oldest
         *
         * Analytics/cache order:
         *
         * oldest -> newest
         */
        List<MarketCandle> ordered =
                new ArrayList<>(
                        candles
                );

        Collections.reverse(
                ordered
        );

        return ordered;
    }

    /*
     * =========================================================
     * FIND ALL CANDLES
     * =========================================================
     */
    @Transactional(readOnly = true)
    public List<MarketCandle> findCandles(
            String instrumentKey,
            String timeframe) {

        if (instrumentKey == null
                || instrumentKey.isBlank()
                || timeframe == null
                || timeframe.isBlank()) {

            return List.of();
        }

        return repository
                .findByInstrumentKeyAndTimeframeAndTimestampBetweenOrderByTimestampAsc(
                        instrumentKey,
                        timeframe,
                        Instant.MIN,
                        Instant.MAX
                )
                .stream()
                .map(this::toMarketCandle)
                .toList();
    }

    /*
     * =========================================================
     * FIND CANDLES BY DATE/TIME RANGE
     * =========================================================
     */
    @Transactional(readOnly = true)
    public List<MarketCandle> findCandles(
            String instrumentKey,
            String timeframe,
            Instant from,
            Instant to) {

        if (instrumentKey == null
                || instrumentKey.isBlank()
                || timeframe == null
                || timeframe.isBlank()
                || from == null
                || to == null) {

            return List.of();
        }

        if (from.isAfter(to)) {

            log.warn(
                    "Invalid candle time range: from={}, to={}",
                    from,
                    to
            );

            return List.of();
        }

        return repository
                .findByInstrumentKeyAndTimeframeAndTimestampBetweenOrderByTimestampAsc(
                        instrumentKey,
                        timeframe,
                        from,
                        to
                )
                .stream()
                .map(this::toMarketCandle)
                .toList();
    }

    /*
     * =========================================================
     * CHECK CANDLE EXISTS
     * =========================================================
     */
    @Transactional(readOnly = true)
    public boolean exists(
            String instrumentKey,
            String timeframe,
            Instant timestamp) {

        if (instrumentKey == null
                || instrumentKey.isBlank()
                || timeframe == null
                || timeframe.isBlank()
                || timestamp == null) {

            return false;
        }

        return repository
                .existsByInstrumentKeyAndTimeframeAndTimestamp(
                        instrumentKey,
                        timeframe,
                        timestamp
                );
    }

    /*
     * =========================================================
     * CONVERT DOMAIN -> ENTITY
     * =========================================================
     */
    private MarketCandleEntity toEntity(
            MarketCandle candle) {

        MarketCandleEntity entity =
                new MarketCandleEntity();

        entity.setInstrumentKey(
                candle.instrumentKey()
        );

        entity.setTimeframe(
                candle.timeframe()
        );

        entity.setOpen(
                candle.open()
        );

        entity.setHigh(
                candle.high()
        );

        entity.setLow(
                candle.low()
        );

        entity.setClose(
                candle.close()
        );

        entity.setVolume(
                candle.volume()
        );

        entity.setTimestamp(
                candle.timestamp()
        );

        return entity;
    }

    /*
     * =========================================================
     * CONVERT ENTITY -> DOMAIN
     * =========================================================
     */
    private MarketCandle toMarketCandle(
            MarketCandleEntity entity) {

        return new MarketCandle(
                entity.getInstrumentKey(),
                entity.getTimeframe(),
                entity.getOpen(),
                entity.getHigh(),
                entity.getLow(),
                entity.getClose(),
                entity.getVolume(),
                entity.getTimestamp()
        );
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