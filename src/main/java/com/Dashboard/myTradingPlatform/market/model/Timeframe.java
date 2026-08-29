package com.Dashboard.myTradingPlatform.market.model;

public enum Timeframe {

    I1("I1", 1),
    I5("I5", 5),
    I15("I15", 15),
    D1("1d", 1440);

    private final String value;
    private final int minutes;

    Timeframe(String value, int minutes) {
        this.value = value;
        this.minutes = minutes;
    }

    public String getValue() {
        return value;
    }

    public int getMinutes() {
        return minutes;
    }

    public static Timeframe fromValue(String value) {

        for (Timeframe timeframe : values()) {

            if (timeframe.value.equalsIgnoreCase(value)) {
                return timeframe;
            }
        }

        throw new IllegalArgumentException(
                "Unsupported timeframe: " + value
        );
    }
}