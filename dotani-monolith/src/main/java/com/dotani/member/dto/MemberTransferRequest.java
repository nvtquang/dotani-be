package com.dotani.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** @param memberId the member to move; null means the caller themself */
public record MemberTransferRequest(
        @Size(max = 36)
        String memberId,

        @NotBlank
        @Size(max = 36)
        String toOrganizationId,

        @Size(max = 1000)
        String reason
) {
}
