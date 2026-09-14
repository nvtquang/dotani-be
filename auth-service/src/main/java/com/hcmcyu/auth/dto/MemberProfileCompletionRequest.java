package com.hcmcyu.auth.dto;

import java.time.LocalDate;

public record MemberProfileCompletionRequest(
        String fullName,
        String phone,
        LocalDate dateOfBirth
) {
}
