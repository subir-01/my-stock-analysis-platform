package com.Dashboard.myTradingPlatform.market.initializer;

import com.Dashboard.myTradingPlatform.market.analytics.model.MarketInstrument;
import com.Dashboard.myTradingPlatform.market.repository.MarketInstrumentRepository;
import com.Dashboard.myTradingPlatform.market.event.MarketInstrumentEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@Order(0)
public class MarketInstrumentInitializer {

    private final MarketInstrumentRepository repository;

    public MarketInstrumentInitializer(
            MarketInstrumentRepository repository) {

        this.repository = repository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeInstruments() {

        log.info("Starting market instrument initialization");

        List<MarketInstrument> instruments = List.of(

                new MarketInstrument(
                        "NSE_EQ|INE002A01018",
                        "Reliance",
                        "NSE",
                        true
                ),

                new MarketInstrument(
                        "GLOBAL_INDEX|SGX NIFTY",
                        "SGX NIFTY",
                        "GLOBAL",
                        true
                )
        );

        for (MarketInstrument instrument : instruments) {

            if (repository.existsByInstrumentKey(
                    instrument.instrumentKey())) {

                log.debug(
                        "Instrument already exists: {}",
                        instrument.instrumentKey()
                );

                continue;
            }

            MarketInstrumentEntity entity =
                    new MarketInstrumentEntity();

            entity.setInstrumentKey(
                    instrument.instrumentKey()
            );

            entity.setDisplayName(
                    instrument.displayName()
            );

            entity.setExchange(
                    instrument.exchange()
            );

            entity.setEnabled(
                    instrument.enabled()
            );

            repository.save(entity);

            log.info(
                    "Instrument added: {}",
                    instrument.instrumentKey()
            );
        }

        log.info("Market instrument initialization completed");
    }
}