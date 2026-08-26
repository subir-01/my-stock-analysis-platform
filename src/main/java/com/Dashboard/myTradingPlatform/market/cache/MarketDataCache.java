package com.Dashboard.myTradingPlatform.market.cache;

import com.Dashboard.myTradingPlatform.market.model.MarketData;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MarketDataCache {

    private final Map<String, MarketData> marketData =
            new ConcurrentHashMap<>();

    public void update(MarketData data) {
        marketData.put(data.instrumentKey(), data);
    }

    public MarketData get(String instrumentKey) {
        return marketData.get(instrumentKey);
    }
}