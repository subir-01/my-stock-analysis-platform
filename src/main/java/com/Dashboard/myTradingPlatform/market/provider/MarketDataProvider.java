package com.Dashboard.myTradingPlatform.market.provider;

import com.Dashboard.myTradingPlatform.market.model.MarketData;

public interface MarketDataProvider {

    MarketData getLtp(String instrumentKey);
}
