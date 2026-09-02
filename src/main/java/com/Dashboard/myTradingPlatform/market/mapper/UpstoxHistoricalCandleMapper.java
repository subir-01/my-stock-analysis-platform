package com.Dashboard.myTradingPlatform.market.mapper;

import com.Dashboard.myTradingPlatform.market.model.MarketCandle;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class UpstoxHistoricalCandleMapper {

    private final ObjectMapper objectMapper;

    public UpstoxHistoricalCandleMapper(
            ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;
    }

    /*
     * =========================================================
     * MAP UPSTOX RESPONSE
     * =========================================================
     *
     * Upstox historical candle format:
     *
     * [
     *   [
     *     timestamp,
     *     open,
     *     high,
     *     low,
     *     close,
     *     volume,
     *     oi
     *   ]
     * ]
     *
     * Example:
     *
     * [
     *   "2026-08-27T09:59:00+05:30",
     *   1284.0,
     *   1288.0,
     *   1283.0,
     *   1287.0,
     *   123456,
     *   0
     * ]
     */
    public List<MarketCandle> toMarketCandles(
            String instrumentKey,
            String timeframe,
            String response) {

        if (instrumentKey == null
                || instrumentKey.isBlank()) {

            throw new IllegalArgumentException(
                    "Instrument key must not be null or blank"
            );
        }

        if (timeframe == null
                || timeframe.isBlank()) {

            throw new IllegalArgumentException(
                    "Timeframe must not be null or blank"
            );
        }

        if (response == null
                || response.isBlank()) {

            return List.of();
        }

        try {

            JsonNode root =
                    objectMapper.readTree(response);

            /*
             * Expected response:
             *
             * {
             *   "status": "success",
             *   "data": {
             *     "candles": [...]
             *   }
             * }
             */
            JsonNode candlesNode =
                    root.path("data")
                            .path("candles");

            if (!candlesNode.isArray()) {

                log.warn(
                        "Historical response does not contain candles array: instrument={}, timeframe={}",
                        instrumentKey,
                        timeframe
                );

                return List.of();
            }

            List<MarketCandle> candles =
                    new ArrayList<>();

            for (JsonNode candleNode :
                    candlesNode) {

                try {

                    MarketCandle candle =
                            mapCandle(
                                    instrumentKey,
                                    timeframe,
                                    candleNode
                            );

                    if (candle != null) {
                        candles.add(candle);
                    }

                } catch (Exception e) {

                    log.warn(
                            "Skipping invalid historical candle: instrument={}, timeframe={}, candle={}",
                            instrumentKey,
                            timeframe,
                            candleNode,
                            e
                    );
                }
            }

            /*
             * Upstox may return newest -> oldest.
             *
             * Our application expects:
             *
             * oldest -> newest
             */
            candles.sort(
                    java.util.Comparator.comparing(
                            MarketCandle::timestamp
                    )
            );

            log.debug(
                    "Historical candles mapped: instrument={}, timeframe={}, count={}",
                    instrumentKey,
                    timeframe,
                    candles.size()
            );

            return candles;

        } catch (Exception e) {

            log.error(
                    "Failed to map Upstox historical response: instrument={}, timeframe={}",
                    instrumentKey,
                    timeframe,
                    e
            );

            throw new IllegalArgumentException(
                    "Unable to parse Upstox historical candle response",
                    e
            );
        }
    }

    /*
     * =========================================================
     * MAP SINGLE CANDLE
     * =========================================================
     */
    private MarketCandle mapCandle(
            String instrumentKey,
            String timeframe,
            JsonNode candleNode) {

        if (candleNode == null
                || !candleNode.isArray()
                || candleNode.size() < 6) {

            return null;
        }

        /*
         * -----------------------------------------------------
         * INDEX MAPPING
         * -----------------------------------------------------
         *
         * 0 -> timestamp
         * 1 -> open
         * 2 -> high
         * 3 -> low
         * 4 -> close
         * 5 -> volume
         * 6 -> OI (not required by MarketCandle)
         */
        String timestampValue =
                candleNode.get(0).asText();

        if (timestampValue == null
                || timestampValue.isBlank()) {

            return null;
        }

        Instant timestamp =
                Instant.parse(
                        timestampValue
                );

        BigDecimal open =
                decimalValue(
                        candleNode.get(1)
                );

        BigDecimal high =
                decimalValue(
                        candleNode.get(2)
                );

        BigDecimal low =
                decimalValue(
                        candleNode.get(3)
                );

        BigDecimal close =
                decimalValue(
                        candleNode.get(4)
                );

        long volume =
                candleNode.get(5).asLong();

        /*
         * Validate OHLC.
         */
        if (open == null
                || high == null
                || low == null
                || close == null) {

            return null;
        }

        return new MarketCandle(
                instrumentKey,
                timeframe,
                open,
                high,
                low,
                close,
                volume,
                timestamp
        );
    }

    /*
     * =========================================================
     * BIGDECIMAL CONVERSION
     * =========================================================
     */
    private BigDecimal decimalValue(
            JsonNode node) {

        if (node == null
                || node.isNull()) {

            return null;
        }

        try {

            return node.decimalValue();

        } catch (Exception e) {

            return null;
        }
    }
}