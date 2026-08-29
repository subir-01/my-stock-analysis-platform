package com.Dashboard.myTradingPlatform.market.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "market_instrument")
@Getter
@Setter
public class MarketInstrumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "instrument_key",
            nullable = false,
            unique = true,
            length = 100
    )
    private String instrumentKey;

    @Column(
            name = "display_name",
            nullable = false,
            length = 100
    )
    private String displayName;

    @Column(
            name = "exchange",
            nullable = false,
            length = 30
    )
    private String exchange;

    @Column(
            name = "enabled",
            nullable = false
    )
    private boolean enabled;
}