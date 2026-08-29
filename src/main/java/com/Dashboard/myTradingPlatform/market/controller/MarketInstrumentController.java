package com.Dashboard.myTradingPlatform.market.controller;

import com.Dashboard.myTradingPlatform.market.analytics.model.MarketInstrument;
import com.Dashboard.myTradingPlatform.market.service.MarketInstrumentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/market/instruments")
public class MarketInstrumentController {

    private final MarketInstrumentService marketInstrumentService;

    public MarketInstrumentController(
            MarketInstrumentService marketInstrumentService) {

        this.marketInstrumentService =
                marketInstrumentService;
    }

    @GetMapping
    public List<MarketInstrument> getAllInstruments() {

        return marketInstrumentService.getAllInstruments();
    }

    @GetMapping("/enabled")
    public List<MarketInstrument> getEnabledInstruments() {

        return marketInstrumentService.getEnabledInstruments();
    }

    @PostMapping
    public MarketInstrument addInstrument(
            @RequestBody MarketInstrument instrument) {

        return marketInstrumentService.addInstrument(
                instrument
        );
    }

    @PatchMapping("/{id}/enabled")
    public MarketInstrument updateEnabled(
            @PathVariable Long id,
            @RequestParam boolean enabled) {

        return marketInstrumentService.updateEnabled(
                id,
                enabled
        );
    }
}