package com.Dashboard.myTradingPlatform.market.initializer;

import com.Dashboard.myTradingPlatform.market.client.UpstoxMarketDataStreamer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Order(3)
public class MarketDataStreamingInitializer {

    private final UpstoxMarketDataStreamer marketDataStreamer;

    public MarketDataStreamingInitializer(
            UpstoxMarketDataStreamer marketDataStreamer) {

        this.marketDataStreamer = marketDataStreamer;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startStreaming() {

        log.info(
                "Starting live market data streaming"
        );

        marketDataStreamer.start();

        log.info(
                "Live market data streaming initialization completed"
        );
    }
}