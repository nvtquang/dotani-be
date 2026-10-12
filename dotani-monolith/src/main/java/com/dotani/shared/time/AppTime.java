package com.dotani.shared.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Instants are stored and exchanged in UTC. Calendar dates (filters such as "events on 2026-12-01",
 * "posts of the last 7 days") are days in Vietnam, so they are converted with this zone.
 */
public final class AppTime {

    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private AppTime() {
    }

    public static Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(ZONE).toInstant();
    }

    public static Instant startOfNextDay(LocalDate date) {
        return startOfDay(date.plusDays(1));
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }
}
