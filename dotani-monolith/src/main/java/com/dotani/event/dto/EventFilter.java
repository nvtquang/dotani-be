package com.dotani.event.dto;

import com.dotani.event.entity.EventStatus;
import com.dotani.event.entity.EventType;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Event list filters; every field is optional.
 *
 * @param when          UPCOMING (starts now or later) or PAST (already started)
 * @param from          inclusive lower bound on startTime (calendar view)
 * @param to            exclusive upper bound on startTime (calendar view)
 * @param participating only events the current member registered GOING for
 */
public record EventFilter(
        EventType type,
        String organizationId,
        EventStatus status,
        LocalDate date,
        When when,
        Instant from,
        Instant to,
        boolean participating
) {
    public enum When { UPCOMING, PAST }
}
