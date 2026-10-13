package com.dotani.fee.dto;

import com.dotani.fee.entity.FeePeriodStatus;
import com.dotani.fee.entity.PaymentMethod;
import java.time.Instant;
import java.time.LocalDate;

/**
 * @param collector      where members transfer the fee; null when no collector is set
 * @param summary        payment progress over the members the caller oversees; null for members
 * @param myPayment      the caller's own status; null when the period does not apply to the caller
 */
public record FeePeriodResponse(
        String id,
        String name,
        String description,
        String organizationId,
        String organizationName,
        long amount,
        LocalDate dueDate,
        FeePeriodStatus status,
        Collector collector,
        Summary summary,
        MyPayment myPayment,
        boolean canManage,
        Instant lastRemindedAt,
        Instant createdAt
) {

    /** Bank details of the collecting officer (the QR Banking set up in their profile). */
    public record Collector(
            String memberId,
            String fullName,
            String bankName,
            String bankCode,
            String accountNumber,
            String accountHolderName,
            String qrImageUrl
    ) {
    }

    public record Summary(long expectedCount, long paidCount, long unpaidCount, long collectedAmount, long expectedAmount) {
    }

    public record MyPayment(boolean paid, long amount, PaymentMethod method, Instant paidAt) {
    }
}
