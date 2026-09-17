package com.hcmcyu.member.dto;

import jakarta.validation.constraints.Size;

public record MemberBankingRequest(
        @Size(max = 255)
        String bankName,

        @Size(max = 50)
        String bankCode,

        @Size(max = 50)
        String accountNumber,

        @Size(max = 255)
        String accountHolderName
) {
}
