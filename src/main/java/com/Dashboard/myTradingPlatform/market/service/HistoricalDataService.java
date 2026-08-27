package com.Dashboard.myTradingPlatform.market.service;

import com.Dashboard.myTradingPlatform.market.client.UpstoxHistoricalDataClient;
import com.Dashboard.myTradingPlatform.market.mapper.UpstoxHistoricalCandleMapper;
import com.Dashboard.myTradingPlatform.market.model.MarketCandle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class HistoricalDataService {
    private final UpstoxHistoricalDataClient historicalDataClient;
    private final UpstoxHistoricalCandleMapper historicalCandleMapper;
    private final MarketCandleService marketCandleService;

    public HistoricalDataService(
            UpstoxHistoricalDataClient historicalDataClient,
            UpstoxHistoricalCandleMapper historicalCandleMapper,
            MarketCandleService marketCandleService) {
        this.historicalDataClient = historicalDataClient;
        this.historicalCandleMapper = historicalCandleMapper;
        this.marketCandleService = marketCandleService;
    }

    public void loadHistoricalData(
            String instrumentKey,
            String timeframe,
            int interval,
            String fromDate,
            String toDate) {

        log.info(
                "Loading historical data: instrument={}, timeframe={}, from={}, to={}",
                instrumentKey,
                timeframe,
                fromDate,
                toDate
        );

        String response = historicalDataClient.getHistoricalCandles(
                instrumentKey,
                interval,
                toDate,
                fromDate
        );

        List<MarketCandle> candles =
                historicalCandleMapper.toMarketCandles(
                        instrumentKey,
                        timeframe,
                        response
                );

        log.info(
                "Historical candles received: instrument={}, timeframe={}, count={}",
                instrumentKey,
                timeframe,
                candles.size()
        );

        for (MarketCandle candle : candles) {
            marketCandleService.processCandle(candle);
        }

        log.info(
                "Historical data loaded: instrument={}, timeframe={}, count={}",
                instrumentKey,
                timeframe,
                candles.size()
        );
    }
}