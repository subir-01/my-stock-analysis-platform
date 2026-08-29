package com.Dashboard.myTradingPlatform.market.client;

import com.Dashboard.myTradingPlatform.market.analytics.model.MarketInstrument;
import com.Dashboard.myTradingPlatform.market.model.MarketCandle;
import com.Dashboard.myTradingPlatform.market.model.MarketData;
import com.Dashboard.myTradingPlatform.market.service.MarketCandleService;
import com.Dashboard.myTradingPlatform.market.service.MarketDataService;
import com.Dashboard.myTradingPlatform.market.service.MarketInstrumentService;
import com.upstox.ApiClient;
import com.upstox.Configuration;
import com.upstox.auth.OAuth;
import com.upstox.feeder.MarketDataStreamerV3;
import com.upstox.feeder.MarketUpdateV3;
import com.upstox.feeder.constants.Mode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@Slf4j
public class UpstoxMarketDataStreamer {

    private final MarketDataService marketDataService;
    private final MarketCandleService marketCandleService;
    private final MarketInstrumentService marketInstrumentService;

    private final String accessToken;

    /*
     * Keeps the latest Upstox feed for every instrument.
     */
    private final Map<String, MarketUpdateV3.Feed> feeds =
            new ConcurrentHashMap<>();

    private MarketDataStreamerV3 streamer;

    /*
     * Prevent accidental multiple WebSocket connections.
     */
    private volatile boolean started = false;

    public UpstoxMarketDataStreamer(
            MarketDataService marketDataService,
            MarketCandleService marketCandleService,
            MarketInstrumentService marketInstrumentService,
            @Value("${upstox.access-token}") String accessToken) {

        this.marketDataService = marketDataService;
        this.marketCandleService = marketCandleService;
        this.marketInstrumentService = marketInstrumentService;
        this.accessToken = accessToken;
    }

    /*
     * =========================================================
     * START WEBSOCKET
     * =========================================================
     */
    public synchronized void start() {

        if (started) {

            log.warn(
                    "Upstox WebSocket is already started"
            );

            return;
        }

        Set<String> instrumentKeys =
                marketInstrumentService
                        .getEnabledInstruments()
                        .stream()
                        .filter(instrument -> instrument != null)
                        .filter(MarketInstrument::enabled)
                        .map(MarketInstrument::instrumentKey)
                        .filter(key -> key != null && !key.isBlank())
                        .collect(Collectors.toCollection(HashSet::new));

        if (instrumentKeys.isEmpty()) {

            log.warn(
                    "No enabled market instruments found. WebSocket will not start."
            );

            return;
        }

        log.info(
                "Starting Upstox WebSocket for {} instruments",
                instrumentKeys.size()
        );

        log.info(
                "Instrument keys for subscription: {}",
                instrumentKeys
        );

        /*
         * =====================================================
         * CONFIGURE UPSTOX API CLIENT
         * =====================================================
         */
        ApiClient apiClient =
                Configuration.getDefaultApiClient();

        OAuth oauth =
                (OAuth) apiClient.getAuthentication(
                        "OAUTH2"
                );

        oauth.setAccessToken(
                accessToken
        );

        /*
         * =====================================================
         * CREATE STREAMER
         * =====================================================
         */
        streamer =
                new MarketDataStreamerV3(
                        apiClient,
                        instrumentKeys,
                        Mode.FULL
                );

        streamer.autoReconnect(
                true,
                5,
                30
        );

        /*
         * =====================================================
         * ON OPEN
         * =====================================================
         */
        streamer.setOnOpenListener(() -> {

            log.info(
                    "Upstox WebSocket connection opened"
            );

            log.info(
                    "Subscribing to {} instruments",
                    instrumentKeys.size()
            );

            streamer.subscribe(
                    instrumentKeys,
                    Mode.FULL
            );

            log.info(
                    "Market data subscription request sent"
            );
        });

        /*
         * =====================================================
         * ON MARKET UPDATE
         * =====================================================
         */
        streamer.setOnMarketUpdateListener(
                this::processMarketUpdate
        );

        started = true;

        log.info(
                "Connecting to Upstox market data WebSocket"
        );

        streamer.connect();
    }

    /*
     * =========================================================
     * PROCESS MARKET UPDATE
     * =========================================================
     */
    private void processMarketUpdate(
            MarketUpdateV3 marketUpdate) {

        if (marketUpdate == null) {

            log.debug(
                    "Received null market update"
            );

            return;
        }

        log.debug(
                "Market update received from Upstox"
        );

        Map<String, MarketUpdateV3.Feed> marketFeeds =
                marketUpdate.getFeeds();

        if (marketFeeds == null
                || marketFeeds.isEmpty()) {

            log.debug(
                    "Market update contains no instrument feeds"
            );

            return;
        }

        /*
         * Store latest feed for every instrument.
         */
        feeds.putAll(
                marketFeeds
        );

        /*
         * Process every instrument independently.
         */
        marketFeeds.forEach(
                this::processFeed
        );
    }

    /*
     * =========================================================
     * PROCESS SINGLE INSTRUMENT FEED
     * =========================================================
     */
    private void processFeed(
            String instrumentKey,
            MarketUpdateV3.Feed feed) {

        if (feed == null) {
            return;
        }

        log.debug(
                "Processing feed for instrument: {}",
                instrumentKey
        );

        log.debug(
                "Instrument feed details: {}",
                feed
        );

        MarketUpdateV3.FullFeed fullFeed =
                feed.getFullFeed();

        if (fullFeed == null) {

            log.debug(
                    "No full feed available: instrument={}",
                    instrumentKey
            );

            return;
        }

        /*
         * =====================================================
         * MARKET DATA
         * =====================================================
         */
        processMarketData(
                instrumentKey,
                fullFeed
        );

        /*
         * =====================================================
         * CANDLE DATA
         * =====================================================
         */
        processCandleData(
                instrumentKey,
                fullFeed
        );
    }

    /*
     * =========================================================
     * PROCESS MARKET DATA
     * =========================================================
     */
    private void processMarketData(
            String instrumentKey,
            MarketUpdateV3.FullFeed fullFeed) {

        MarketUpdateV3.LTPC ltpc =
                getLtpc(
                        fullFeed
                );

        if (ltpc == null) {

            log.debug(
                    "No LTPC data available: instrument={}",
                    instrumentKey
            );

            return;
        }

        /*
         * Validate LTP timestamp.
         */
        if (ltpc.getLtt() <= 0) {

            log.debug(
                    "Invalid LTT received: instrument={}, ltt={}",
                    instrumentKey,
                    ltpc.getLtt()
            );

            return;
        }

        MarketData marketData =
                new MarketData(
                        instrumentKey,

                        BigDecimal.valueOf(
                                ltpc.getLtp()
                        ),

                        ltpc.getLtq(),

                        null,

                        BigDecimal.valueOf(
                                ltpc.getCp()
                        ),

                        Instant.ofEpochMilli(
                                ltpc.getLtt()
                        )
                );

        log.info(
                "MarketData received: instrument={}, price={}",
                instrumentKey,
                marketData.lastPrice()
        );

        marketDataService.processMarketData(
                marketData
        );
    }

    /*
     * =========================================================
     * GET LTPC
     * =========================================================
     *
     * Supports both:
     *
     * marketFF
     * indexFF
     */
    private MarketUpdateV3.LTPC getLtpc(
            MarketUpdateV3.FullFeed fullFeed) {

        if (fullFeed.getMarketFF() != null) {

            return fullFeed
                    .getMarketFF()
                    .getLtpc();
        }

        if (fullFeed.getIndexFF() != null) {

            return fullFeed
                    .getIndexFF()
                    .getLtpc();
        }

        return null;
    }

    /*
     * =========================================================
     * PROCESS CANDLE DATA
     * =========================================================
     */
    private void processCandleData(
            String instrumentKey,
            MarketUpdateV3.FullFeed fullFeed) {

        MarketUpdateV3.MarketOHLC marketOHLC =
                getMarketOHLC(
                        fullFeed
                );

        if (marketOHLC == null) {

            log.debug(
                    "No OHLC data available: instrument={}",
                    instrumentKey
            );

            return;
        }

        if (marketOHLC.getOhlc() == null
                || marketOHLC.getOhlc().isEmpty()) {

            log.debug(
                    "OHLC list is empty: instrument={}",
                    instrumentKey
            );

            return;
        }

        for (MarketUpdateV3.OHLC ohlc :
                marketOHLC.getOhlc()) {

            processSingleCandle(
                    instrumentKey,
                    ohlc
            );
        }
    }

    /*
     * =========================================================
     * GET MARKET OHLC
     * =========================================================
     */
    private MarketUpdateV3.MarketOHLC getMarketOHLC(
            MarketUpdateV3.FullFeed fullFeed) {

        if (fullFeed.getMarketFF() != null) {

            return fullFeed
                    .getMarketFF()
                    .getMarketOHLC();
        }

        if (fullFeed.getIndexFF() != null) {

            return fullFeed
                    .getIndexFF()
                    .getMarketOHLC();
        }

        return null;
    }

    /*
     * =========================================================
     * PROCESS SINGLE CANDLE
     * =========================================================
     */
    private void processSingleCandle(
            String instrumentKey,
            MarketUpdateV3.OHLC ohlc) {

        if (ohlc == null) {
            return;
        }

        String interval =
                ohlc.getInterval();

        if (interval == null
                || interval.isBlank()) {

            return;
        }

        if (ohlc.getTs() <= 0) {

            log.debug(
                    "Invalid candle timestamp: instrument={}, timeframe={}, ts={}",
                    instrumentKey,
                    interval,
                    ohlc.getTs()
            );

            return;
        }

        /*
         * Upstox can return multiple OHLC intervals.
         *
         * Example:
         *
         * 1d
         * I1
         */
        MarketCandle candle =
                new MarketCandle(
                        instrumentKey,

                        interval,

                        BigDecimal.valueOf(
                                ohlc.getOpen()
                        ),

                        BigDecimal.valueOf(
                                ohlc.getHigh()
                        ),

                        BigDecimal.valueOf(
                                ohlc.getLow()
                        ),

                        BigDecimal.valueOf(
                                ohlc.getClose()
                        ),

                        ohlc.getVol(),

                        Instant.ofEpochMilli(
                                ohlc.getTs()
                        )
                );

        log.info(
                "Candle received: instrument={}, timeframe={}, close={}",
                instrumentKey,
                interval,
                candle.close()
        );

        /*
         * IMPORTANT:
         *
         * MarketCandleService decides whether this candle is:
         *
         * - currently forming
         * - completed
         * - out of order
         *
         * and therefore whether it should be persisted.
         */
        marketCandleService.processCandle(
                candle
        );
    }

    /*
     * =========================================================
     * GET LATEST FEEDS
     * =========================================================
     */
    public Map<String, MarketUpdateV3.Feed> getFeeds() {

        return Collections.unmodifiableMap(
                feeds
        );
    }

    /*
     * =========================================================
     * SET FEEDS
     * =========================================================
     */
    public void setFeeds(
            Map<String, MarketUpdateV3.Feed> feeds) {

        this.feeds.clear();

        if (feeds != null) {

            this.feeds.putAll(
                    feeds
            );
        }
    }

    /*
     * =========================================================
     * GET SINGLE FEED
     * =========================================================
     */
    public MarketUpdateV3.Feed getFeed(
            String instrumentKey) {

        if (instrumentKey == null
                || instrumentKey.isBlank()) {

            return null;
        }

        return feeds.get(
                instrumentKey
        );
    }

    /*
     * =========================================================
     * IS STARTED
     * =========================================================
     */
    public boolean isStarted() {

        return started;
    }
}