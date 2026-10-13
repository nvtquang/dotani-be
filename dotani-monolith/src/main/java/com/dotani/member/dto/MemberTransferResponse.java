package com.dotani.member.dto;

import com.dotani.member.entity.TransferStatus;
import java.time.Instant;

public record MemberTransferResponse(
        String id,
        String memberId,
        String memberName,
        String memberAvatarUrl,
        String fromOrganizationId,
        String fromOrganizationName,
        String toOrganizationId,
        String toOrganizationName,
        String reason,
        TransferStatus status,
        String requestedById,
        String requestedByName,
        Instant requestedAt,
        String decidedById,
        String decidedByName,
        Instant decidedAt,
        String decisionNote,
        boolean canDecide,
        boolean canCancel
) {
}
