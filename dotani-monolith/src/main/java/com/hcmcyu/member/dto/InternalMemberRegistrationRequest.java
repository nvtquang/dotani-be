package com.hcmcyu.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record InternalMemberRegistrationRequest(
        @NotBlank
        @Size(max = 36)
        String userId,

        @NotBlank
        @Size(max = 255)
        String fullName,

        @Email
        @Size(max = 255)
        String email,

        @Size(max = 30)
        String phone,

        LocalDate dateOfBirth,

        @NotBlank
        @Size(max = 36)
        String organizationId
) {
}
