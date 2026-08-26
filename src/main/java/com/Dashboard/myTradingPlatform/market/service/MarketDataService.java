package com.Dashboard.myTradingPlatform.market.service;

import com.Dashboard.myTradingPlatform.market.cache.MarketDataCache;
import com.Dashboard.myTradingPlatform.market.event.MarketDataEvent;
import com.Dashboard.myTradingPlatform.market.model.MarketData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MarketDataService {

    private final MarketDataCache marketDataCache;
    private final ApplicationEventPublisher eventPublisher;

    public MarketDataService(
            MarketDataCache marketDataCache,
            ApplicationEventPublisher eventPublisher
    ) {
        this.marketDataCache = marketDataCache;
        this.eventPublisher = eventPublisher;
    }

    public void processMarketData(MarketData marketData) {

        log.info(
                "Processing market data: instrument={}, price={}",
                marketData.instrumentKey(),
                marketData.lastPrice()
        );

        marketDataCache.update(marketData);

        eventPublisher.publishEvent(
                new MarketDataEvent(marketData)
        );

        log.debug(
                "Market data cached and event published: {}",
                marketData
        );
    }
}