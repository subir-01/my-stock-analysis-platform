package com.Dashboard.myTradingPlatform.market.service;

import com.Dashboard.myTradingPlatform.market.model.Instrument;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class InstrumentSubscriptionService {

    private final Map<String, Instrument> instruments =
            new ConcurrentHashMap<>();

    @PostConstruct
    public void initialize() {

        addInstrument(
                new Instrument(
                        "NSE_EQ|INE002A01018",
                        "RELIANCE",
                        "NSE",
                        "EQUITY"
                )
        );

        addInstrument(
                new Instrument(
                        "GLOBAL_INDEX|SGX NIFTY",
                        "GIFT NIFTY",
                        "GLOBAL",
                        "INDEX"
                )
        );
    }

    public void addInstrument(Instrument instrument) {
        instruments.put(
                instrument.instrumentKey(),
                instrument
        );

        log.info(
                "Instrument added: symbol={}, key={}, type={}",
                instrument.symbol(),
                instrument.instrumentKey(),
                instrument.type()
        );
    }

    public void removeInstrument(String instrumentKey) {
        Instrument removed =
                instruments.remove(instrumentKey);

        if (removed != null) {
            log.info(
                    "Instrument removed: symbol={}, key={}",
                    removed.symbol(),
                    removed.instrumentKey()
            );
        }
    }

    public Set<String> getInstrumentKeys() {
        return Set.copyOf(instruments.keySet());
    }

    public Set<Instrument> getInstruments() {
        return Set.copyOf(instruments.values());
    }
}