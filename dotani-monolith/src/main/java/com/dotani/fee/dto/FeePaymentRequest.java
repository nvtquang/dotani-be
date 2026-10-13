package com.dotani.fee.dto;

import com.dotani.fee.entity.PaymentMethod;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * @param amount null means the period's amount
 * @param paidAt null means now
 */
public record FeePaymentRequest(
        @NotBlank
        @Size(max = 36)
        String memberId,

        @Min(1)
        @Max(100_000_000)
        Long amount,

        @NotNull
        PaymentMethod method,

        Instant paidAt,

        @Size(max = 500)
        String note
) {
}
