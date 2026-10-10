package com.dotani.member.dto;

public record OrganizationMemberCountResponse(
        String organizationId,
        String organizationName,
        long count
) {
}
