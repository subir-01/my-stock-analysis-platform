package com.Dashboard.myTradingPlatform.market.client;

import com.upstox.ApiClient;
import com.upstox.feeder.MarketDataStreamerV3;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UpstoxMarketDataStreamer {

    private final MarketDataStreamerV3 streamer;

    public UpstoxMarketDataStreamer(ApiClient apiClient) {

        this.streamer = new MarketDataStreamerV3(apiClient);
        streamer.setOnOpenListener(() ->
                log.info("Upstox WebSocket connection opened")
        );
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
}