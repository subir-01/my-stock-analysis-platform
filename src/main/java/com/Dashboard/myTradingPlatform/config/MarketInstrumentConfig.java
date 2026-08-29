package com.Dashboard.myTradingPlatform.config;

import com.Dashboard.myTradingPlatform.market.analytics.model.MarketInstrument;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class MarketInstrumentConfig {

    @Bean
    public List<MarketInstrument> marketInstruments() {

        return List.of(

                new MarketInstrument(
                        "NSE_EQ|INE002A01018",
                        "Reliance",
                        "NSE",
                        true
                ),

                new MarketInstrument(
                        "GLOBAL_INDEX|SGX NIFTY",
                        "SGX NIFTY",
                        "GLOBAL",
                        true
                )

        );
    }
}