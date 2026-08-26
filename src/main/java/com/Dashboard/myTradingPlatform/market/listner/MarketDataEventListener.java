package com.Dashboard.myTradingPlatform.market.listener;

import com.Dashboard.myTradingPlatform.market.event.MarketDataEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MarketDataEventListener {

    @EventListener
    public void onMarketData(MarketDataEvent event) {

        log.info(
                "MarketDataEvent received: instrument={}, price={}",
                event.marketData().instrumentKey(),
                event.marketData().lastPrice()
        );

        log.debug(
                "MarketDataEvent details: {}",
                event.marketData()
        );
    }
}