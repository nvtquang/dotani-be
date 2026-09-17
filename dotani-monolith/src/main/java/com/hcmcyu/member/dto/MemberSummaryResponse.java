package com.hcmcyu.member.dto;

public record MemberSummaryResponse(
        String memberId,
        String fullName,
        String organizationId
) {
}

