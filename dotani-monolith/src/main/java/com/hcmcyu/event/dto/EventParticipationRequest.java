package com.hcmcyu.event.dto;

import com.hcmcyu.event.entity.ParticipationStatus;
import jakarta.validation.constraints.NotNull;

public record EventParticipationRequest(
        @NotNull
        ParticipationStatus status
) {
}
