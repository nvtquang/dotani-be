package com.hcmcyu.member.dto;

public record MemberBankingResponse(
        String memberId,
        String fullName,
        String bankName,
        String bankCode,
        String accountNumber,
        String accountHolderName,
        String bankQrImageUrl
) {
}
