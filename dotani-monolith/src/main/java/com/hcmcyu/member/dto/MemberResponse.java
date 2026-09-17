package com.hcmcyu.member.dto;

import com.hcmcyu.member.entity.Gender;
import com.hcmcyu.member.entity.MemberRole;
import com.hcmcyu.member.entity.MemberStatus;
import java.time.Instant;
import java.time.LocalDate;

public record MemberResponse(
        String id,
        String userId,
        String fullName,
        LocalDate dateOfBirth,
        Gender gender,
        String phone,
        String email,
        String address,
        String avatarUrl,
        LocalDate youthUnionJoinDate,
        MemberStatus memberStatus,
        MemberRole memberRole,
        String organizationId,
        String organizationName,
        Instant createdAt,
        Instant updatedAt
) {
}

