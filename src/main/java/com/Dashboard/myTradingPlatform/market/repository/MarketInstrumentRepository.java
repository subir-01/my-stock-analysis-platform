package com.Dashboard.myTradingPlatform.market.repository;

import com.Dashboard.myTradingPlatform.market.event.MarketInstrumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MarketInstrumentRepository
        extends JpaRepository<MarketInstrumentEntity, Long> {

    Optional<MarketInstrumentEntity> findByInstrumentKey(
            String instrumentKey
    );

    List<MarketInstrumentEntity> findByEnabledTrue();

    boolean existsByInstrumentKey(
            String instrumentKey
    );
}