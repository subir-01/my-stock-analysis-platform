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

        this.restClient =
                RestClient.builder()
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

        log.info(
                "Upstox Historical Data Client initialized"
        );
    }

    /*
     * =========================================================
     * GENERIC HISTORICAL CANDLE REQUEST
     * =========================================================
     *
     * Examples:
     *
     * I1:
     * /v3/historical-candle/{instrument}/minutes/1/{to}/{from}
     *
     * I5:
     * /v3/historical-candle/{instrument}/minutes/5/{to}/{from}
     *
     * I15:
     * /v3/historical-candle/{instrument}/minutes/15/{to}/{from}
     *
     * D1:
     * /v3/historical-candle/{instrument}/days/1/{to}/{from}
     */
    public String getHistoricalCandles(
            String instrumentKey,
            String unit,
            int interval,
            String toDate,
            String fromDate) {

        log.info(
                "Fetching historical candles: instrument={}, unit={}, interval={}, from={}, to={}",
                instrumentKey,
                unit,
                interval,
                fromDate,
                toDate
        );

        return restClient
                .get()
                .uri(uriBuilder ->
                        uriBuilder
                                .path(
                                        "/v3/historical-candle/{instrumentKey}/{unit}/{interval}/{toDate}/{fromDate}"
                                )
                                .build(
                                        instrumentKey,
                                        unit,
                                        interval,
                                        toDate,
                                        fromDate
                                )
                )
                .retrieve()
                .body(String.class);
    }

    /*
     * =========================================================
     * MINUTE HISTORICAL DATA
     * =========================================================
     */
    public String getMinuteHistoricalCandles(
            String instrumentKey,
            int interval,
            String toDate,
            String fromDate) {

        return getHistoricalCandles(
                instrumentKey,
                "minutes",
                interval,
                toDate,
                fromDate
        );
    }

    /*
     * =========================================================
     * DAILY HISTORICAL DATA
     * =========================================================
     */
    public String getDailyHistoricalCandles(
            String instrumentKey,
            String toDate,
            String fromDate) {

        return getHistoricalCandles(
                instrumentKey,
                "days",
                1,
                toDate,
                fromDate
        );
    }

    /*
     * =========================================================
     * TEST I1
     * =========================================================
     */
    public void testOneMinuteHistoricalData() {

        String response =
                getMinuteHistoricalCandles(
                        "NSE_EQ|INE002A01018",
                        1,
                        "2026-08-27",
                        "2026-08-01"
                );

        log.info(
                "1-minute historical API response: {}",
                response
        );
    }

    /*
     * =========================================================
     * TEST I5
     * =========================================================
     */
    public void testFiveMinuteHistoricalData() {

        String response =
                getMinuteHistoricalCandles(
                        "NSE_EQ|INE002A01018",
                        5,
                        "2026-08-27",
                        "2026-08-01"
                );

        log.info(
                "5-minute historical API response: {}",
                response
        );
    }

    /*
     * =========================================================
     * TEST I15
     * =========================================================
     */
    public void testFifteenMinuteHistoricalData() {

        String response =
                getMinuteHistoricalCandles(
                        "NSE_EQ|INE002A01018",
                        15,
                        "2026-08-27",
                        "2026-08-01"
                );

        log.info(
                "15-minute historical API response: {}",
                response
        );
    }

    /*
     * =========================================================
     * TEST DAILY
     * =========================================================
     */
    public void testDailyHistoricalData() {

        String response =
                getDailyHistoricalCandles(
                        "NSE_EQ|INE002A01018",
                        "2026-08-27",
                        "2026-08-01"
                );

        log.info(
                "Daily historical API response: {}",
                response
        );
    }
}