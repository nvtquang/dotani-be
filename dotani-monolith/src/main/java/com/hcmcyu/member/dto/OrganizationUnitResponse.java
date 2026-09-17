package com.hcmcyu.member.dto;

import com.hcmcyu.member.entity.OrganizationUnitType;

public record OrganizationUnitResponse(
        String id,
        String name,
        String code,
        OrganizationUnitType type,
        String parentId,
        boolean active
) {
}

