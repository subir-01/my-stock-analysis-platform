package com.Dashboard.myTradingPlatform.market.service;

import com.Dashboard.myTradingPlatform.market.cache.MarketCandleCache;
import com.Dashboard.myTradingPlatform.market.model.MarketCandle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MarketCandleService {

    private final MarketCandleCache marketCandleCache;

    public MarketCandleService(MarketCandleCache marketCandleCache) {
        this.marketCandleCache = marketCandleCache;
    }

    public void processCandle(MarketCandle candle) {

        log.info(
                "Processing candle: instrument={}, timeframe={}, close={}",
                candle.instrumentKey(),
                candle.timeframe(),
                candle.close()
        );

        marketCandleCache.update(candle);

        log.debug(
                "Candle cached: {}",
                candle
        );
    }
}