package com.hcmcyu.member.dto;

import com.hcmcyu.member.entity.Gender;
import com.hcmcyu.member.entity.MemberStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record MemberRequest(
        @Size(max = 36)
        String userId,

        @NotBlank
        @Size(max = 255)
        String fullName,

        @Past
        LocalDate dateOfBirth,

        Gender gender,

        @Size(max = 30)
        String phone,

        @Email
        @Size(max = 255)
        String email,

        @Size(max = 500)
        String address,

        @Size(max = 1000)
        String avatarUrl,

        LocalDate youthUnionJoinDate,

        MemberStatus memberStatus,

        @NotBlank
        String organizationId
) {
}
