package com.Dashboard.myTradingPlatform.market.service;

import com.Dashboard.myTradingPlatform.market.model.MarketCandle;
import com.Dashboard.myTradingPlatform.market.repository.MarketCandleRepository;
import com.Dashboard.myTradingPlatform.market.event.MarketCandleEntity;
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

    private final MarketCandleRepository repository;

    public MarketCandlePersistenceService(
            MarketCandleRepository repository) {

        this.repository = repository;
    }

    /*
     * =========================================================
     * SAVE SINGLE CANDLE
     * =========================================================
     */
    @Transactional
    public boolean save(
            MarketCandle candle) {

        if (candle == null) {
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
     * SAVE MULTIPLE CANDLES
     * =========================================================
     */
    @Transactional
    public void saveAll(
            List<MarketCandle> candles) {

        if (candles == null
                || candles.isEmpty()) {

            return;
        }

        int savedCount = 0;
        int duplicateCount = 0;
        int invalidCount = 0;

        for (MarketCandle candle : candles) {

            if (candle == null
                    || candle.instrumentKey() == null
                    || candle.instrumentKey().isBlank()
                    || candle.timeframe() == null
                    || candle.timeframe().isBlank()
                    || candle.timestamp() == null) {

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
     * COUNT CANDLES
     * =========================================================
     */
    @Transactional(readOnly = true)
    public long countCandles(
            String instrumentKey,
            String timeframe) {

        return repository.countByInstrumentKeyAndTimeframe(
                instrumentKey,
                timeframe
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

        return repository
                .findTopByInstrumentKeyAndTimeframeOrderByTimestampDesc(
                        instrumentKey,
                        timeframe
                )
                .map(this::toMarketCandle);
    }

    /*
     * =========================================================
     * FIND LATEST CANDLES
     * =========================================================
     */
    @Transactional(readOnly = true)
    public List<MarketCandle> findLatestCandles(
            String instrumentKey,
            String timeframe,
            int limit) {

        if (limit <= 0) {
            return List.of();
        }

        List<MarketCandleEntity> entities =
                repository
                        .findTop500ByInstrumentKeyAndTimeframeOrderByTimestampDesc(
                                instrumentKey,
                                timeframe
                        )
                        .stream()
                        .limit(
                                Math.min(limit, 500)
                        )
                        .toList();

        List<MarketCandle> candles =
                entities.stream()
                        .map(this::toMarketCandle)
                        .toList();

        List<MarketCandle> ordered =
                new ArrayList<>(candles);

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
     * FIND CANDLES BY DATE RANGE
     * =========================================================
     */
    @Transactional(readOnly = true)
    public List<MarketCandle> findCandles(
            String instrumentKey,
            String timeframe,
            Instant from,
            Instant to) {

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
     * ENTITY MAPPER
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
     * DOMAIN MAPPER
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
}