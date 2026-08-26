package com.Dashboard.myTradingPlatform.market.client;

import com.Dashboard.myTradingPlatform.market.mapper.UpstoxMarketCandleMapper;
import com.Dashboard.myTradingPlatform.market.mapper.UpstoxMarketDataMapper;
import com.Dashboard.myTradingPlatform.market.model.MarketCandle;
import com.Dashboard.myTradingPlatform.market.model.MarketData;
import com.Dashboard.myTradingPlatform.market.service.MarketDataService;
import com.upstox.ApiClient;
import com.upstox.feeder.MarketDataStreamerV3;
import com.upstox.feeder.constants.Mode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.Dashboard.myTradingPlatform.market.service.MarketCandleService;
import java.util.Set;

@Component
@Slf4j
public class UpstoxMarketDataStreamer {

    private static final String RELIANCE = "NSE_EQ|INE002A01018";

    private final MarketDataStreamerV3 streamer;
    private final UpstoxMarketDataMapper marketDataMapper;
    private final UpstoxMarketCandleMapper marketCandleMapper;
    private final MarketDataService marketDataService;
    private final MarketCandleService marketCandleService;


    public UpstoxMarketDataStreamer(
            ApiClient apiClient,
            UpstoxMarketDataMapper marketDataMapper,
            UpstoxMarketCandleMapper marketCandleMapper,
            MarketDataService marketDataService, MarketCandleService marketCandleService) {

        this.streamer = new MarketDataStreamerV3(apiClient);
        this.marketDataMapper = marketDataMapper;
        this.marketCandleMapper = marketCandleMapper;
        this.marketDataService = marketDataService;
        this.marketCandleService = marketCandleService;

        streamer.setOnMarketUpdateListener(marketUpdate -> {
            log.info("Market update received from Upstox");
            log.debug("Market update type: {}", marketUpdate.getType());

            if (marketUpdate.getFeeds() == null) {
                log.debug("Market update contains no instrument feeds");
                return;
            }

            marketUpdate.getFeeds().forEach((instrumentKey, feed) -> {
                log.debug("Processing feed for instrument: {}", instrumentKey);

                MarketData marketData = marketDataMapper.toMarketData(
                        instrumentKey,
                        feed
                );

                if (marketData != null) {
                    log.info(
                            "MarketData received: instrument={}, price={}",
                            marketData.instrumentKey(),
                            marketData.lastPrice()
                    );

                    log.debug("Complete MarketData: {}", marketData);
                    marketDataService.processMarketData(marketData);
                }

                for (MarketCandle candle : marketCandleMapper.toMarketCandles(
                        instrumentKey,
                        feed)) {

                    log.info(
                            "Candle received: instrument={}, timeframe={}, close={}",
                            candle.instrumentKey(),
                            candle.timeframe(),
                            candle.close()
                    );

                    marketCandleService.processCandle(candle);
                }
            });
        });

        streamer.setOnOpenListener(() -> {
            log.info("Upstox WebSocket connection opened");
            subscribeToMarketData();
        });

        streamer.setOnErrorListener(error ->
                log.error("Upstox WebSocket error: {}", error)
        );

        log.info("Upstox Market Data Streamer initialized");
        connect();
    }

    private void connect() {
        log.info("Connecting to Upstox Market Data WebSocket...");
        streamer.connect();
    }

    private void subscribeToMarketData() {
        log.info("Subscribing to market data for: {}", RELIANCE);

        Set<String> instrumentKeys = Set.of(RELIANCE);

        streamer.subscribe(
                instrumentKeys,
                Mode.FULL
        );

        log.info("Market data subscription request sent");
    }
}