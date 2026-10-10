package com.dotani.event.dto;

import com.dotani.event.entity.ParticipationStatus;
import jakarta.validation.constraints.NotNull;

public record EventParticipationRequest(
        @NotNull
        ParticipationStatus status
) {
}
