package com.hcmcyu.event.dto;

import java.util.List;

public record EventDashboardSummaryResponse(
        long upcomingEventCount,
        long registeredParticipantCount,
        List<EventResponse> upcomingEvents,
        List<EventResponse> registeredEvents
) {
}
