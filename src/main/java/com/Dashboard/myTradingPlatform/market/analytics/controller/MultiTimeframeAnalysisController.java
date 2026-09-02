package com.Dashboard.myTradingPlatform.market.analytics.controller;

import com.Dashboard.myTradingPlatform.market.analytics.model.MultiTimeframeAnalysis;
import com.Dashboard.myTradingPlatform.market.analytics.service.MultiTimeframeAnalysisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MultiTimeframeAnalysisController {

    private static final String DEFAULT_INSTRUMENT_KEY =
            "NSE_EQ|INE002A01018";

    private final MultiTimeframeAnalysisService
            multiTimeframeAnalysisService;

    public MultiTimeframeAnalysisController(
            MultiTimeframeAnalysisService multiTimeframeAnalysisService) {

        this.multiTimeframeAnalysisService =
                multiTimeframeAnalysisService;
    }



    /*
     * =========================================================
     * MULTI-TIMEFRAME ANALYSIS
     * =========================================================
     *
     * Example:
     *
     * GET /api/market/multi-timeframe
     *
     * Or:
     *
     * GET /api/market/multi-timeframe
     *     ?instrumentKey=NSE_EQ|INE002A01018
     */
    @GetMapping("/api/market/multi-timeframe")
    public MultiTimeframeAnalysis getMultiTimeframeAnalysis(
            @RequestParam(
                    required = false
            )
            String instrumentKey) {

        String resolvedInstrumentKey =
                instrumentKey == null
                        || instrumentKey.isBlank()
                        ? DEFAULT_INSTRUMENT_KEY
                        : instrumentKey;

        return multiTimeframeAnalysisService.analyze(
                resolvedInstrumentKey
        );
    }

    /*
     * =========================================================
     * GET CONFIGURED TIMEFRAMES
     * =========================================================
     *
     * Example response:
     *
     * [
     *     "1d",
     *     "I15",
     *     "I5",
     *     "I1"
     * ]
     */
    @GetMapping("/api/market/timeframes")
    public List<String> getTimeframes() {

        return multiTimeframeAnalysisService
                .getTimeframes();
    }
}