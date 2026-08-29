package com.Dashboard.myTradingPlatform.market.service;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

@Service
public class TradingCalendarService {

    /*
     * NSE holidays can be added here.
     *
     * We will move this to database/config later.
     */
    private static final Set<LocalDate> NSE_HOLIDAYS =
            Set.of();

    /*
     * ---------------------------------------------------------
     * Return the last expected trading date.
     * ---------------------------------------------------------
     *
     * Example:
     *
     * Sunday 30-Aug
     *       ↓
     * Friday 28-Aug
     *
     * Monday 31-Aug
     *       ↓
     * Monday 31-Aug
     */
    public LocalDate getLastExpectedTradingDate(
            LocalDate date) {

        LocalDate result = date;

        while (!isTradingDay(result)) {

            result = result.minusDays(1);
        }

        return result;
    }

    /*
     * ---------------------------------------------------------
     * Check trading day.
     * ---------------------------------------------------------
     */
    public boolean isTradingDay(
            LocalDate date) {

        if (date == null) {
            return false;
        }

        DayOfWeek day =
                date.getDayOfWeek();

        /*
         * Saturday/Sunday are not trading days.
         */
        if (day == DayOfWeek.SATURDAY
                || day == DayOfWeek.SUNDAY) {

            return false;
        }

        /*
         * Holiday check.
         */
        return !NSE_HOLIDAYS.contains(date);
    }
}