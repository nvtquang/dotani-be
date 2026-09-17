package com.hcmcyu.member.dto;

public record MemberDisplayNameResponse(
        String memberId,
        String fullName
) {
}
