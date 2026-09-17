package com.hcmcyu.event.dto;

import com.hcmcyu.event.entity.EventStatus;
import com.hcmcyu.event.entity.EventType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record EventRequest(
        @NotBlank
        @Size(max = 255)
        String title,

        String description,

        @NotNull
        EventType type,

        @Size(max = 255)
        String location,

        @NotNull
        LocalDateTime startTime,

        @NotNull
        LocalDateTime endTime,

        LocalDateTime registrationDeadline,

        @NotBlank
        @Size(max = 36)
        String organizationId,

        @Min(1)
        Integer maxParticipants,

        @NotNull
        EventStatus status
) {
}
