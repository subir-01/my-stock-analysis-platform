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

        if (accessToken == null
                || accessToken.isBlank()) {

            throw new IllegalStateException(
                    "UPSTOX_ACCESS_TOKEN is not configured"
            );
        }

        this.restClient =
                RestClient.builder()
                        .baseUrl(
                                "https://api.upstox.com"
                        )
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
     * GET HISTORICAL CANDLES
     * =========================================================
     *
     * Supported examples:
     *
     * minutes / 1
     * minutes / 5
     * minutes / 15
     * days / 1
     *
     * The URL generated is:
     *
     * /v3/historical-candle/{instrumentKey}
     * /{unit}/{interval}/{toDate}/{fromDate}
     */
    public String getHistoricalCandles(
            String instrumentKey,
            String unit,
            int interval,
            String toDate,
            String fromDate) {

        validateRequest(
                instrumentKey,
                unit,
                interval,
                toDate,
                fromDate
        );

        log.info(
                "Fetching historical candles: instrument={}, unit={}, interval={}, from={}, to={}",
                instrumentKey,
                unit,
                interval,
                fromDate,
                toDate
        );

        try {

            String response =
                    restClient.get()
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

            if (response == null
                    || response.isBlank()) {

                log.warn(
                        "Empty historical API response: instrument={}, unit={}, interval={}, from={}, to={}",
                        instrumentKey,
                        unit,
                        interval,
                        fromDate,
                        toDate
                );

                return null;
            }

            log.debug(
                    "Historical API response received: instrument={}, unit={}, interval={}",
                    instrumentKey,
                    unit,
                    interval
            );

            return response;

        } catch (Exception e) {

            log.error(
                    "Failed to fetch historical candles: instrument={}, unit={}, interval={}, from={}, to={}",
                    instrumentKey,
                    unit,
                    interval,
                    fromDate,
                    toDate,
                    e
            );

            throw e;
        }
    }


    /*
     * =========================================================
     * GET TODAY'S INTRADAY CANDLES
     * =========================================================
     *
     * Upstox endpoint:
     *
     * /v3/historical-candle/intraday/{instrumentKey}/minutes/{interval}
     *
     * This endpoint is used for today's intraday candles.
     *
     * For our application we currently use:
     *
     * minutes / 1
     *
     * to obtain today's I1 candles.
     *
     * Those I1 candles will later be used to reconstruct
     * the current I5 and I15 aggregation buckets.
     */
    public String getIntradayCandles(
            String instrumentKey,
            int interval) {

        if (instrumentKey == null
                || instrumentKey.isBlank()) {

            throw new IllegalArgumentException(
                    "Instrument key must not be null or blank"
            );
        }

        if (interval <= 0) {

            throw new IllegalArgumentException(
                    "Intraday interval must be greater than zero"
            );
        }

        log.info(
                "Fetching intraday candles: instrument={}, interval={}",
                instrumentKey,
                interval
        );

        try {

            String response =
                    restClient.get()
                            .uri(uriBuilder ->
                                    uriBuilder
                                            .path(
                                                    "/v3/historical-candle/intraday/{instrumentKey}/minutes/{interval}"
                                            )
                                            .build(
                                                    instrumentKey,
                                                    interval
                                            )
                            )
                            .retrieve()
                            .body(String.class);

            if (response == null
                    || response.isBlank()) {

                log.warn(
                        "Empty intraday API response: instrument={}, interval={}",
                        instrumentKey,
                        interval
                );

                return null;
            }

            log.info(
                    "Intraday candles received: instrument={}, interval={}",
                    instrumentKey,
                    interval
            );

            return response;

        } catch (Exception e) {

            log.error(
                    "Failed to fetch intraday candles: instrument={}, interval={}",
                    instrumentKey,
                    interval,
                    e
            );

            throw e;
        }
    }


    /*
     * =========================================================
     * VALIDATION
     * =========================================================
     */

    private void validateRequest(
            String instrumentKey,
            String unit,
            int interval,
            String toDate,
            String fromDate) {

        if (instrumentKey == null
                || instrumentKey.isBlank()) {

            throw new IllegalArgumentException(
                    "Instrument key must not be null or blank"
            );
        }

        if (unit == null
                || unit.isBlank()) {

            throw new IllegalArgumentException(
                    "Historical data unit must not be null or blank"
            );
        }

        if (interval <= 0) {

            throw new IllegalArgumentException(
                    "Historical data interval must be greater than zero"
            );
        }

        if (toDate == null
                || toDate.isBlank()) {

            throw new IllegalArgumentException(
                    "To date must not be null or blank"
            );
        }

        if (fromDate == null
                || fromDate.isBlank()) {

            throw new IllegalArgumentException(
                    "From date must not be null or blank"
            );
        }
    }


    /*
     * =========================================================
     * TEST HISTORICAL DATA
     * =========================================================
     *
     * This method is only for manual testing.
     *
     * It can be called from a test/controller if required.
     */

    public void testHistoricalData() {

        String response =
                getHistoricalCandles(
                        "NSE_EQ|INE002A01018",
                        "minutes",
                        1,
                        "2026-08-27",
                        "2026-08-01"
                );

        log.info(
                "Historical API response: {}",
                response
        );
    }
}