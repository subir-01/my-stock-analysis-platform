package com.Dashboard.myTradingPlatform.market.service;

import com.Dashboard.myTradingPlatform.market.analytics.model.MarketInstrument;
import com.Dashboard.myTradingPlatform.market.event.MarketInstrumentEntity;
import com.Dashboard.myTradingPlatform.market.repository.MarketInstrumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class MarketInstrumentService {

    private final MarketInstrumentRepository repository;

    public MarketInstrumentService(
            MarketInstrumentRepository repository) {

        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<MarketInstrument> getAllInstruments() {

        return repository.findAll()
                .stream()
                .map(this::toModel)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MarketInstrument> getEnabledInstruments() {

        return repository.findByEnabledTrue()
                .stream()
                .map(this::toModel)
                .toList();
    }

    @Transactional
    public MarketInstrument addInstrument(
            MarketInstrument instrument) {

        if (repository.existsByInstrumentKey(
                instrument.instrumentKey())) {

            throw new IllegalArgumentException(
                    "Instrument already exists: "
                            + instrument.instrumentKey()
            );
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

        return toModel(
                repository.save(entity)
        );
    }

    @Transactional
    public MarketInstrument updateEnabled(
            Long id,
            boolean enabled) {

        MarketInstrumentEntity entity =
                repository.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Instrument not found: " + id
                                )
                        );

        entity.setEnabled(enabled);

        return toModel(
                repository.save(entity)
        );
    }

    private MarketInstrument toModel(
            MarketInstrumentEntity entity) {

        return new MarketInstrument(
                entity.getInstrumentKey(),
                entity.getDisplayName(),
                entity.getExchange(),
                entity.isEnabled()
        );
    }
}