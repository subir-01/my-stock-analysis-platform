package com.Dashboard.myTradingPlatform.market.controller;

import com.Dashboard.myTradingPlatform.market.service.HistoricalDataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HistoricalDataTestController {
    private final HistoricalDataService historicalDataService;

    public HistoricalDataTestController(
            HistoricalDataService historicalDataService) {
        this.historicalDataService = historicalDataService;
    }

    @GetMapping("/api/test/historical")
    public String testHistoricalData() {
        historicalDataService.loadHistoricalData(
                "NSE_EQ|INE002A01018",
                "I1",
                1,
                "2026-08-01",
                "2026-08-27"
        );

        return "Historical data loaded successfully.";
    }
}