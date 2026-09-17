package com.hcmcyu.member.dto;

public record MemberDirectoryResponse(
        String id,
        String fullName,
        String organizationId,
        String organizationName,
        String avatarUrl
) {
}
