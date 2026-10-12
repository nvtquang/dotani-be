package com.dotani.event.dto;

import com.dotani.event.entity.EventStatus;
import com.dotani.event.entity.EventType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

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
        Instant startTime,

        @NotNull
        Instant endTime,

        Instant registrationDeadline,

        @NotBlank
        @Size(max = 36)
        String organizationId,

        @Min(1)
        Integer maxParticipants,

        @NotNull
        EventStatus status
) {
}
