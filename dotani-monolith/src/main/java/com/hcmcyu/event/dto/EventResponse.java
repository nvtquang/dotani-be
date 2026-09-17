package com.hcmcyu.event.dto;

import com.hcmcyu.event.entity.EventStatus;
import com.hcmcyu.event.entity.EventType;
import java.time.LocalDateTime;

public record EventResponse(
        String id,
        String title,
        String description,
        EventType type,
        String location,
        LocalDateTime startTime,
        LocalDateTime endTime,
        LocalDateTime registrationDeadline,
        String organizationId,
        Integer maxParticipants,
        EventStatus status,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
