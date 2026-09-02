package com.Dashboard.myTradingPlatform.market.analytics.controller;

import com.Dashboard.myTradingPlatform.market.analytics.calculator.MarketSignalAnalyzer;
import com.Dashboard.myTradingPlatform.market.analytics.model.MarketSignal;
import com.Dashboard.myTradingPlatform.market.analytics.model.MultiTimeframeAnalysis;
import com.Dashboard.myTradingPlatform.market.analytics.service.MultiTimeframeAnalysisService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MarketSignalController {

    private static final String DEFAULT_INSTRUMENT_KEY =
            "NSE_INDEX|Nifty 50";

    private final MultiTimeframeAnalysisService
            multiTimeframeAnalysisService;

    private final MarketSignalAnalyzer
            marketSignalAnalyzer;

    public MarketSignalController(
            MultiTimeframeAnalysisService multiTimeframeAnalysisService,
            MarketSignalAnalyzer marketSignalAnalyzer) {

        this.multiTimeframeAnalysisService =
                multiTimeframeAnalysisService;

        this.marketSignalAnalyzer =
                marketSignalAnalyzer;
    }

    /*
     * =========================================================
     * MARKET SIGNAL
     * =========================================================
     *
     * GET:
     *
     * /api/market/signal
     *
     * Optional:
     *
     * /api/market/signal?instrumentKey=NSE_EQ|INE002A01018
     *
     * Flow:
     *
     * Instrument
     *      ↓
     * Multi-Timeframe Analysis
     *      ↓
     * Market Signal Analyzer
     *      ↓
     * Market Signal
     */
    @GetMapping("/api/market/signal")
    public MarketSignal getMarketSignal(
            @RequestParam(required = false)
            String instrumentKey) {

        String resolvedInstrumentKey =
                resolveInstrumentKey(
                        instrumentKey
                );

        MultiTimeframeAnalysis analysis =
                multiTimeframeAnalysisService.analyze(
                        resolvedInstrumentKey
                );

        return marketSignalAnalyzer.analyze(
                analysis
        );
    }

    /*
     * =========================================================
     * COMPLETE MARKET ANALYSIS + SIGNAL
     * =========================================================
     *
     * Useful during development.
     *
     * Returns:
     *
     * {
     *     "analysis": {...},
     *     "signal": {...}
     * }
     */
    @GetMapping("/api/market/signal-analysis")
    public MarketSignalAnalysisResponse getSignalAnalysis(
            @RequestParam(required = false)
            String instrumentKey) {

        String resolvedInstrumentKey =
                resolveInstrumentKey(
                        instrumentKey
                );

        MultiTimeframeAnalysis analysis =
                multiTimeframeAnalysisService.analyze(
                        resolvedInstrumentKey
                );

        MarketSignal signal =
                marketSignalAnalyzer.analyze(
                        analysis
                );

        return new MarketSignalAnalysisResponse(
                analysis,
                signal
        );
    }

    /*
     * =========================================================
     * RESOLVE INSTRUMENT
     * =========================================================
     */
    private String resolveInstrumentKey(
            String instrumentKey) {

        if (instrumentKey == null
                || instrumentKey.isBlank()) {

            return DEFAULT_INSTRUMENT_KEY;
        }

        return instrumentKey.trim();
    }

    /*
     * =========================================================
     * RESPONSE MODEL
     * =========================================================
     */
    public record MarketSignalAnalysisResponse(

            MultiTimeframeAnalysis analysis,

            MarketSignal signal

    ) {
    }
}