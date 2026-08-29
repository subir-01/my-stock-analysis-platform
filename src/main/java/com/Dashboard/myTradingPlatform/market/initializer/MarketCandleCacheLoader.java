package com.Dashboard.myTradingPlatform.market.initializer;

import com.Dashboard.myTradingPlatform.market.analytics.model.MarketInstrument;
import com.Dashboard.myTradingPlatform.market.cache.MarketCandleCache;
import com.Dashboard.myTradingPlatform.market.model.MarketCandle;
import com.Dashboard.myTradingPlatform.market.service.MarketCandlePersistenceService;
import com.Dashboard.myTradingPlatform.market.service.MarketCandleService;
import com.Dashboard.myTradingPlatform.market.service.MarketInstrumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@Order(2)
public class MarketCandleCacheLoader {

    private static final int CACHE_SIZE = 500;

    private final MarketCandlePersistenceService persistenceService;
    private final MarketCandleCache marketCandleCache;
    private final MarketCandleService marketCandleService;
    private final MarketInstrumentService marketInstrumentService;

    public MarketCandleCacheLoader(
            MarketCandlePersistenceService persistenceService,
            MarketCandleCache marketCandleCache,
            MarketCandleService marketCandleService,
            MarketInstrumentService marketInstrumentService) {

        this.persistenceService =
                persistenceService;

        this.marketCandleCache =
                marketCandleCache;

        this.marketCandleService =
                marketCandleService;

        this.marketInstrumentService =
                marketInstrumentService;
    }

    /*
     * =========================================================
     * APPLICATION STARTUP
     * =========================================================
     */
    @EventListener(ApplicationReadyEvent.class)
    public void loadCache() {

        log.info(
                "Starting market candle cache initialization"
        );

        List<MarketInstrument> instruments =
                marketInstrumentService.getEnabledInstruments();

        if (instruments == null
                || instruments.isEmpty()) {

            log.warn(
                    "No enabled market instruments found. Cache initialization skipped."
            );

            return;
        }

        log.info(
                "Found {} enabled market instruments for cache initialization",
                instruments.size()
        );

        for (MarketInstrument instrument : instruments) {

            if (instrument == null) {
                continue;
            }

            if (!instrument.enabled()) {
                continue;
            }

            String instrumentKey =
                    instrument.instrumentKey();

            if (instrumentKey == null
                    || instrumentKey.isBlank()) {

                log.warn(
                        "Skipping instrument with empty instrument key"
                );

                continue;
            }

            /*
             * Load I1 candles.
             */
            loadInstrument(
                    instrumentKey,
                    "I1"
            );
        }

        log.info(
                "Market candle cache initialization completed"
        );
    }

    /*
     * =========================================================
     * LOAD ONE INSTRUMENT
     * =========================================================
     */
    private void loadInstrument(
            String instrumentKey,
            String timeframe) {

        log.info(
                "Loading candles into cache: instrument={}, timeframe={}",
                instrumentKey,
                timeframe
        );

        try {

            /*
             * Get latest candles from DB.
             */
            List<MarketCandle> candles =
                    persistenceService.findLatestCandles(
                            instrumentKey,
                            timeframe,
                            CACHE_SIZE
                    );

            if (candles == null
                    || candles.isEmpty()) {

                log.warn(
                        "No historical candles found in DB: instrument={}, timeframe={}",
                        instrumentKey,
                        timeframe
                );

                return;
            }

            /*
             * =================================================
             * POPULATE CACHE
             * =================================================
             */
            for (MarketCandle candle : candles) {

                if (candle == null) {
                    continue;
                }

                marketCandleCache.put(
                        candle
                );
            }

            log.info(
                    "Historical candles loaded into cache: instrument={}, timeframe={}, count={}",
                    instrumentKey,
                    timeframe,
                    candles.size()
            );

            /*
             * =================================================
             * INITIALIZE CURRENT CANDLE
             * =================================================
             *
             * Find the newest candle from the loaded data.
             */
            MarketCandle latestCandle =
                    findLatestCandle(
                            candles
                    );

            if (latestCandle == null) {

                log.warn(
                        "Unable to determine latest candle: instrument={}, timeframe={}",
                        instrumentKey,
                        timeframe
                );

                return;
            }

            /*
             * Initialize MarketCandleService tracker.
             *
             * This prevents the first WebSocket message from
             * incorrectly starting a brand-new current candle.
             */
            marketCandleService.initializeCurrentCandle(
                    latestCandle
            );

            log.info(
                    "Current candle tracker initialized: instrument={}, timeframe={}, timestamp={}",
                    instrumentKey,
                    timeframe,
                    latestCandle.timestamp()
            );

        } catch (Exception e) {

            log.error(
                    "Failed to initialize candle cache: instrument={}, timeframe={}",
                    instrumentKey,
                    timeframe,
                    e
            );
        }
    }

    /*
     * =========================================================
     * FIND LATEST CANDLE
     * =========================================================
     *
     * We determine the latest candle ourselves instead of
     * assuming that the database returned order is correct.
     */
    private MarketCandle findLatestCandle(
            List<MarketCandle> candles) {

        if (candles == null
                || candles.isEmpty()) {

            return null;
        }

        Optional<MarketCandle> latest =
                candles.stream()
                        .filter(candle -> candle != null)
                        .max(
                                (c1, c2) ->
                                        c1.timestamp()
                                                .compareTo(
                                                        c2.timestamp()
                                                )
                        );

        return latest.orElse(null);
    }
}