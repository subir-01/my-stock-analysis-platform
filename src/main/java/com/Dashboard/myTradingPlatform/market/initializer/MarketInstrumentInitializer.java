package com.Dashboard.myTradingPlatform.market.initializer;

import com.Dashboard.myTradingPlatform.market.analytics.model.MarketInstrument;
import com.Dashboard.myTradingPlatform.market.repository.MarketInstrumentRepository;
import com.Dashboard.myTradingPlatform.market.event.MarketInstrumentEntity;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class MarketInstrumentInitializer {

    private final MarketInstrumentRepository repository;

    public MarketInstrumentInitializer(
            MarketInstrumentRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void initializeInstruments() {

        log.info("Starting market instrument initialization");

        /*
         * Disable previously configured instruments.
         *
         * Current application should use only:
         * NSE_INDEX|Nifty 50
         */
        List<MarketInstrumentEntity> existingInstruments =
                repository.findAll();

        for (MarketInstrumentEntity entity : existingInstruments) {

            if (entity.isEnabled()) {
                entity.setEnabled(false);

                log.info(
                        "Disabled previous instrument: {}",
                        entity.getInstrumentKey()
                );
            }
        }

        repository.saveAll(existingInstruments);

        /*
         * NIFTY 50
         */
        MarketInstrument nifty50 =
                new MarketInstrument(
                        "NSE_INDEX|Nifty 50",
                        "NIFTY 50",
                        "NSE",
                        true
                );

        List<MarketInstrumentEntity> matchingInstruments =
                repository.findAll()
                        .stream()
                        .filter(entity ->
                                entity.getInstrumentKey()
                                        .equals(nifty50.instrumentKey()))
                        .toList();

        if (!matchingInstruments.isEmpty()) {

            MarketInstrumentEntity entity =
                    matchingInstruments.get(0);

            entity.setDisplayName(
                    nifty50.displayName()
            );

            entity.setExchange(
                    nifty50.exchange()
            );

            entity.setEnabled(
                    nifty50.enabled()
            );

            repository.save(entity);

            log.info(
                    "NIFTY 50 instrument enabled: {}",
                    nifty50.instrumentKey()
            );

        } else {

            MarketInstrumentEntity entity =
                    new MarketInstrumentEntity();

            entity.setInstrumentKey(
                    nifty50.instrumentKey()
            );

            entity.setDisplayName(
                    nifty50.displayName()
            );

            entity.setExchange(
                    nifty50.exchange()
            );

            entity.setEnabled(
                    nifty50.enabled()
            );

            repository.save(entity);

            log.info(
                    "NIFTY 50 instrument added: {}",
                    nifty50.instrumentKey()
            );
        }

        log.info("Market instrument initialization completed");
    }
}