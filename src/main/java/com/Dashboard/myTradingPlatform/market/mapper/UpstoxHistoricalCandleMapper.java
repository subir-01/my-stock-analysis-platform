package com.Dashboard.myTradingPlatform.market.mapper;

import com.Dashboard.myTradingPlatform.market.model.MarketCandle;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class UpstoxHistoricalCandleMapper {

    private final ObjectMapper objectMapper;

    public UpstoxHistoricalCandleMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<MarketCandle> toMarketCandles(
            String instrumentKey,
            String timeframe,
            String response) {

        List<MarketCandle> candles = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode candleArray = root
                    .path("data")
                    .path("candles");

            if (!candleArray.isArray()) {
                return candles;
            }

            for (JsonNode candle : candleArray) {

                String timestamp = candle.get(0).asText();

                BigDecimal open =
                        BigDecimal.valueOf(candle.get(1).asDouble());

                BigDecimal high =
                        BigDecimal.valueOf(candle.get(2).asDouble());

                BigDecimal low =
                        BigDecimal.valueOf(candle.get(3).asDouble());

                BigDecimal close =
                        BigDecimal.valueOf(candle.get(4).asDouble());

                long volume =
                        candle.get(5).asLong();



                OffsetDateTime offsetDateTime =
                        OffsetDateTime.parse(timestamp);

                Instant instant =
                        offsetDateTime.toInstant();
                MarketCandle marketCandle =
                        new MarketCandle(
                                instrumentKey,
                                timeframe,
                                open,
                                high,
                                low,
                                close,
                                volume,
                                instant
                        );

                candles.add(marketCandle);
            }

            candles.sort(
                    Comparator.comparing(MarketCandle::timestamp)
            );

            return candles;

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to map historical candle response",
                    e
            );
        }
    }
}