package com.Dashboard.myTradingPlatform.market.mapper;

import com.Dashboard.myTradingPlatform.market.model.MarketCandle;
import com.upstox.feeder.MarketUpdateV3;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class UpstoxMarketCandleMapper {

    public List<MarketCandle> toMarketCandles(
            String instrumentKey,
            MarketUpdateV3.Feed feed
    ) {

        List<MarketCandle> candles = new ArrayList<>();

        if (feed == null || feed.getFullFeed() == null) {
            return candles;
        }

        MarketUpdateV3.MarketFullFeed marketFullFeed =
                feed.getFullFeed().getMarketFF();

        if (marketFullFeed == null ||
                marketFullFeed.getMarketOHLC() == null ||
                marketFullFeed.getMarketOHLC().getOhlc() == null) {
            return candles;
        }

        for (MarketUpdateV3.OHLC ohlc :
                marketFullFeed.getMarketOHLC().getOhlc()) {

            MarketCandle candle = new MarketCandle(
                    instrumentKey,
                    ohlc.getInterval(),
                    BigDecimal.valueOf(ohlc.getOpen()),
                    BigDecimal.valueOf(ohlc.getHigh()),
                    BigDecimal.valueOf(ohlc.getLow()),
                    BigDecimal.valueOf(ohlc.getClose()),
                    ohlc.getVol(),
                    Instant.ofEpochMilli(ohlc.getTs())
            );

            candles.add(candle);
        }

        return candles;
    }
}