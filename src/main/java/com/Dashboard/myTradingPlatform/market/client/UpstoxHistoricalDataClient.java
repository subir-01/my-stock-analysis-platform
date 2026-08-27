package com.Dashboard.myTradingPlatform.market.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class UpstoxHistoricalDataClient {

    private final RestClient restClient;

    public UpstoxHistoricalDataClient(
            @Value("${UPSTOX_ACCESS_TOKEN}") String accessToken) {

        this.restClient = RestClient.builder()
                .baseUrl("https://api.upstox.com")
                .defaultHeader(
                        "Authorization",
                        "Bearer " + accessToken
                )
                .defaultHeader(
                        "Accept",
                        "application/json"
                )
                .defaultHeader(
                        "Api-Version",
                        "2.0"
                )
                .build();

        log.info("Upstox Historical Data Client initialized");
    }

    public String getHistoricalCandles(
            String instrumentKey,
            int interval,
            String toDate,
            String fromDate) {

        log.info(
                "Fetching historical candles: instrument={}, interval={}, from={}, to={}",
                instrumentKey,
                interval,
                fromDate,
                toDate
        );

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v3/historical-candle/{instrumentKey}/minutes/{interval}/{toDate}/{fromDate}")
                        .build(
                                instrumentKey,
                                interval,
                                toDate,
                                fromDate
                        ))
                .retrieve()
                .body(String.class);
    }

    public void testHistoricalData() {

        String response = getHistoricalCandles(
                "NSE_EQ|INE002A01018",
                1,
                "2026-08-27",
                "2026-08-01"
        );

        log.info("Historical API response: {}", response);
    }
}