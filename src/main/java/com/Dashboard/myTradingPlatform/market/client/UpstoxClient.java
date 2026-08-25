package com.Dashboard.myTradingPlatform.market.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class UpstoxClient {

    private final RestClient restClient;

    public UpstoxClient(
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
        log.info("Upstox client initialized");
    }

    public String getLtp(String instrumentKey) {
        log.info("Fetching LTP for instrument: {}", instrumentKey);
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v3/market-quote/ltp")
                        .queryParam("instrument_key", instrumentKey)
                        .build())
                .exchange((request, response) -> {
                    log.debug(
                            "Upstox LTP API response status: {}",
                            response.getStatusCode()
                    );
                    String body = new String(
                            response.getBody().readAllBytes()
                    );
                    log.debug(
                            "Upstox LTP API response received"
                    );
                    return body;
                });
    }
}