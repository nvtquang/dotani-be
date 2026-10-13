package com.dotani.fee.dto;

import com.dotani.fee.entity.PaymentMethod;
import java.time.Instant;

/** A member of the period and whether (and how) they paid. Payment fields are null while unpaid. */
public record FeeMemberStatusResponse(
        String memberId,
        String fullName,
        String organizationId,
        String organizationName,
        String avatarUrl,
        boolean paid,
        String paymentId,
        Long amount,
        PaymentMethod method,
        Instant paidAt,
        String note,
        String recordedByName
) {
}
