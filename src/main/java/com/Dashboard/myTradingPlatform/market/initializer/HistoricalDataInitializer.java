package com.Dashboard.myTradingPlatform.market.initializer;

import com.Dashboard.myTradingPlatform.market.analytics.model.MarketInstrument;
import com.Dashboard.myTradingPlatform.market.service.HistoricalDataService;
import com.Dashboard.myTradingPlatform.market.service.MarketInstrumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@Slf4j
@Order(1)
public class HistoricalDataInitializer {

    private final HistoricalDataService historicalDataService;
    private final MarketInstrumentService marketInstrumentService;

    public HistoricalDataInitializer(
            HistoricalDataService historicalDataService,
            MarketInstrumentService marketInstrumentService) {

        this.historicalDataService =
                historicalDataService;

        this.marketInstrumentService =
                marketInstrumentService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadHistoricalData() {

        log.info(
                "================================================="
        );

        log.info(
                "Starting historical market data initialization"
        );

        log.info(
                "================================================="
        );

        LocalDate toDate =
                LocalDate.now();

        List<MarketInstrument> instruments =
                marketInstrumentService
                        .getEnabledInstruments();

        if (instruments == null
                || instruments.isEmpty()) {

            log.warn(
                    "No enabled market instruments found. Historical initialization skipped."
            );

            return;
        }

        log.info(
                "Found {} enabled market instruments",
                instruments.size()
        );

        for (MarketInstrument instrument : instruments) {

            if (instrument == null
                    || instrument.instrumentKey() == null
                    || instrument.instrumentKey().isBlank()) {

                continue;
            }

            if (!instrument.enabled()) {
                continue;
            }

            String instrumentKey =
                    instrument.instrumentKey();

            /*
             * Currently loading I1.
             *
             * We will add I5/I15/D1 after the complete
             * I1 flow is verified.
             */
            String timeframe = "I1";

            /*
             * 30 days of historical data.
             */
            LocalDate fromDate =
                    toDate.minusDays(30);

            log.info(
                    "Checking historical data: instrument={}, timeframe={}",
                    instrumentKey,
                    timeframe
            );

            try {

                boolean exists =
                        historicalDataService.hasHistoricalData(
                                instrumentKey,
                                timeframe
                        );

                if (exists) {

                    log.info(
                            "Historical data already exists. Skipping download: instrument={}, timeframe={}",
                            instrumentKey,
                            timeframe
                    );

                    continue;
                }

                log.info(
                        "No historical data found. Downloading: instrument={}, timeframe={}, from={}, to={}",
                        instrumentKey,
                        timeframe,
                        fromDate,
                        toDate
                );

                historicalDataService.loadHistoricalData(
                        instrumentKey,
                        timeframe,
                        fromDate.toString(),
                        toDate.toString()
                );

                log.info(
                        "Historical data processing completed: instrument={}, timeframe={}",
                        instrumentKey,
                        timeframe
                );

            } catch (Exception e) {

                log.error(
                        "Failed to initialize historical data: instrument={}, timeframe={}",
                        instrumentKey,
                        timeframe,
                        e
                );
            }
        }

        log.info(
                "================================================="
        );

        log.info(
                "Historical market data initialization completed"
        );

        log.info(
                "================================================="
        );
    }
}