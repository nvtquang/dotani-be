package com.dotani.event.dto;

import com.dotani.event.entity.EventStatus;
import com.dotani.event.entity.EventType;
import java.time.LocalDateTime;
import java.util.List;

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
        List<EventAttachmentResponse> attachments,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
