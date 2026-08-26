package com.Dashboard.myTradingPlatform.market.mapper;

import com.Dashboard.myTradingPlatform.market.model.MarketData;
import com.upstox.feeder.MarketUpdateV3;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

@Component
public class UpstoxMarketDataMapper {

    public MarketData toMarketData(
            String instrumentKey,
            MarketUpdateV3.Feed feed
    ) {

        if (feed == null || feed.getFullFeed() == null) {
            return null;
        }

        MarketUpdateV3.MarketFullFeed marketFullFeed =
                feed.getFullFeed().getMarketFF();

        if (marketFullFeed == null ||
                marketFullFeed.getLtpc() == null) {
            return null;
        }

        MarketUpdateV3.LTPC ltpc =
                marketFullFeed.getLtpc();

        return new MarketData(
                instrumentKey,
                BigDecimal.valueOf(ltpc.getLtp()),
                ltpc.getLtq(),
                null,
                BigDecimal.valueOf(ltpc.getCp()),
                Instant.ofEpochMilli(ltpc.getLtt())
        );
    }
}