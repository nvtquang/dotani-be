package com.dotani.member.dto;

import com.dotani.member.entity.Gender;
import com.dotani.member.entity.MemberRole;
import com.dotani.member.entity.MemberStatus;
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

