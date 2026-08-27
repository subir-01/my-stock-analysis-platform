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
            MarketUpdateV3.Feed feed) {

        List<MarketCandle> candles = new ArrayList<>();

        if (feed == null || feed.getFullFeed() == null) {
            return candles;
        }

        MarketUpdateV3.MarketOHLC marketOHLC = null;

        if (feed.getFullFeed().getMarketFF() != null) {
            marketOHLC = feed.getFullFeed()
                    .getMarketFF()
                    .getMarketOHLC();
        } else if (feed.getFullFeed().getIndexFF() != null) {
            marketOHLC = feed.getFullFeed()
                    .getIndexFF()
                    .getMarketOHLC();
        }

        if (marketOHLC == null || marketOHLC.getOhlc() == null) {
            return candles;
        }

        for (MarketUpdateV3.OHLC ohlc : marketOHLC.getOhlc()) {

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