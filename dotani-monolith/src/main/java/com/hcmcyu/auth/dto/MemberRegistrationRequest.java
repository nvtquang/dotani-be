package com.hcmcyu.auth.dto;

import java.time.LocalDate;

public record MemberRegistrationRequest(
        String userId,
        String fullName,
        String email,
        String phone,
        LocalDate dateOfBirth,
        String organizationId
) {
}
