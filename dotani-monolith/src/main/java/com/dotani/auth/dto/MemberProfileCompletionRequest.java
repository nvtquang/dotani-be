package com.dotani.auth.dto;

import java.time.LocalDate;

public record MemberProfileCompletionRequest(
        String fullName,
        String phone,
        LocalDate dateOfBirth
) {
}
