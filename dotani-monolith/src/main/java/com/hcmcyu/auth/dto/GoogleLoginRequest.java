package com.hcmcyu.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record GoogleLoginRequest(
        @NotBlank
        String idToken,

        @NotBlank
        @Size(max = 255)
        String fullName,

        @NotBlank
        @Size(max = 30)
        String phone,

        @NotNull
        @Past
        LocalDate dateOfBirth,

        @NotBlank
        @Size(max = 36)
        String organizationId
) {
}
