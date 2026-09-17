package com.hcmcyu.event.dto;

import com.hcmcyu.event.entity.EventType;
import com.hcmcyu.event.entity.ParticipationStatus;
import java.time.LocalDateTime;

public record EventParticipationResponse(
        String id,
        String eventId,
        String eventTitle,
        EventType eventType,
        String eventOrganizationId,
        LocalDateTime eventStartTime,
        LocalDateTime eventEndTime,
        String memberId,
        ParticipationStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
