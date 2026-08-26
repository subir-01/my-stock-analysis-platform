package com.Dashboard.myTradingPlatform.market.provider;

import com.Dashboard.myTradingPlatform.market.client.UpstoxClient;
import com.Dashboard.myTradingPlatform.market.model.MarketData;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class UpstoxDataProvider implements MarketDataProvider {

    private final UpstoxClient upstoxClient;
    private final ObjectMapper objectMapper;


    public UpstoxDataProvider(UpstoxClient upstoxClient, ObjectMapper objectMapper) {
        this.upstoxClient = upstoxClient;
        this.objectMapper = objectMapper;
    }


    @Override
    public MarketData getLtp(String instrumentKey) {
        try {
            String response = upstoxClient.getLtp(instrumentKey);

            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.path("data");
            JsonNode instrumentData = data.properties()
                    .iterator()
                    .next()
                    .getValue();

            return new MarketData(
                    instrumentData.path("instrument_token").asText(),
                    BigDecimal.valueOf(instrumentData.path("last_price").asDouble()),
                    instrumentData.path("ltq").asLong(),
                    instrumentData.path("volume").asLong(),
                    BigDecimal.valueOf(instrumentData.path("cp").asDouble()),
                    Instant.now()
            );
        } catch (Exception e){
            throw new RuntimeException("Failed to parse Upstock LTP response " , e);
        }
    }
}