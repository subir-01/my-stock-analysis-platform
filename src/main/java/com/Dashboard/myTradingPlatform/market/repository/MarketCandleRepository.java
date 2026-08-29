package com.Dashboard.myTradingPlatform.market.repository;

import com.Dashboard.myTradingPlatform.market.event.MarketCandleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MarketCandleRepository
        extends JpaRepository<MarketCandleEntity, Long> {

    Optional<MarketCandleEntity>
    findTopByInstrumentKeyAndTimeframeOrderByTimestampDesc(
            String instrumentKey,
            String timeframe
    );

    List<MarketCandleEntity>
    findTop500ByInstrumentKeyAndTimeframeOrderByTimestampDesc(
            String instrumentKey,
            String timeframe
    );

    List<MarketCandleEntity>
    findByInstrumentKeyAndTimeframeAndTimestampBetweenOrderByTimestampAsc(
            String instrumentKey,
            String timeframe,
            Instant from,
            Instant to
    );

    boolean existsByInstrumentKeyAndTimeframeAndTimestamp(
            String instrumentKey,
            String timeframe,
            Instant timestamp
    );
}