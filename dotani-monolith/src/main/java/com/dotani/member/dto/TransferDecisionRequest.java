package com.dotani.member.dto;

import jakarta.validation.constraints.Size;

public record TransferDecisionRequest(
        @Size(max = 1000)
        String note
) {
}
