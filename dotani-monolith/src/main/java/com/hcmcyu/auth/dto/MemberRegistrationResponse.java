package com.hcmcyu.auth.dto;

public record MemberRegistrationResponse(
        String id,
        String userId,
        String fullName,
        String email,
        String organizationId,
        String organizationName
) {
}
