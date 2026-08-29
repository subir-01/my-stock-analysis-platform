package com.Dashboard.myTradingPlatform.market.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "market_candle",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_market_candle_instrument_timeframe_timestamp",
                        columnNames = {
                                "instrument_key",
                                "timeframe",
                                "timestamp"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_market_candle_lookup",
                        columnList = "instrument_key,timeframe,timestamp"
                )
        }
)
@Getter
@Setter
public class MarketCandleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "instrument_key",
            nullable = false,
            length = 100
    )
    private String instrumentKey;

    @Column(
            name = "timeframe",
            nullable = false,
            length = 20
    )
    private String timeframe;

    @Column(
            name = "open",
            nullable = false,
            precision = 19,
            scale = 4
    )
    private BigDecimal open;

    @Column(
            name = "high",
            nullable = false,
            precision = 19,
            scale = 4
    )
    private BigDecimal high;

    @Column(
            name = "low",
            nullable = false,
            precision = 19,
            scale = 4
    )
    private BigDecimal low;

    @Column(
            name = "close",
            nullable = false,
            precision = 19,
            scale = 4
    )
    private BigDecimal close;

    @Column(name = "volume")
    private Long volume;

    @Column(
            name = "timestamp",
            nullable = false
    )
    private Instant timestamp;
}