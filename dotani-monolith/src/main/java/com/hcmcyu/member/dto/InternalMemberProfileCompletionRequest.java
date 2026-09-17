package com.hcmcyu.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record InternalMemberProfileCompletionRequest(
        @NotBlank
        @Size(max = 255)
        String fullName,

        @NotBlank
        @Size(max = 30)
        String phone,

        @Past
        LocalDate dateOfBirth
) {
}
