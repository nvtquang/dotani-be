package com.dotani.event.dto;

import com.dotani.event.entity.EventStatus;
import com.dotani.event.entity.EventType;
import java.time.Instant;
import java.util.List;

public record EventResponse(
        String id,
        String title,
        String description,
        EventType type,
        String location,
        Instant startTime,
        Instant endTime,
        Instant registrationDeadline,
        String organizationId,
        String organizationName,
        Integer maxParticipants,
        // members registered GOING; with maxParticipants gives the seats left
        long goingCount,
        EventStatus status,
        String createdBy,
        List<EventAttachmentResponse> attachments,
        Instant createdAt,
        Instant updatedAt
) {
}
