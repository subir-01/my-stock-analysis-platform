package com.Dashboard.myTradingPlatform.market.initializer;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class MarketDataInitializationState {

    private final AtomicBoolean initialized =
            new AtomicBoolean(false);

    public boolean isInitialized() {
        return initialized.get();
    }

    public void markInitialized() {
        initialized.set(true);
    }

    public void reset() {
        initialized.set(false);
    }
}