package com.dotani.fee.dto;

import com.dotani.fee.entity.FeePeriodStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record FeePeriodRequest(
        @NotBlank
        @Size(max = 255)
        String name,

        @Size(max = 2000)
        String description,

        @NotBlank
        @Size(max = 36)
        String organizationId,

        @NotNull
        @Min(1)
        @Max(100_000_000)
        Long amount,

        LocalDate dueDate,

        @Size(max = 36)
        String collectorMemberId,

        /** null keeps the current status (OPEN for a new period) */
        FeePeriodStatus status
) {
}
