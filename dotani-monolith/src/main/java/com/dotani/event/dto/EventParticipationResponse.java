package com.dotani.event.dto;

import com.dotani.event.entity.EventType;
import com.dotani.event.entity.ParticipationStatus;
import java.time.Instant;

public record EventParticipationResponse(
        String id,
        String eventId,
        String eventTitle,
        EventType eventType,
        String eventOrganizationId,
        Instant eventStartTime,
        Instant eventEndTime,
        String memberId,
        ParticipationStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
