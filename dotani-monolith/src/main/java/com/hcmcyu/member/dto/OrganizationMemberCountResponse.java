package com.hcmcyu.member.dto;

public record OrganizationMemberCountResponse(
        String organizationId,
        String organizationName,
        long count
) {
}
