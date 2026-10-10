package com.dotani.member.dto;

import com.dotani.member.entity.OrganizationUnitType;

public record OrganizationUnitResponse(
        String id,
        String name,
        String code,
        OrganizationUnitType type,
        String parentId,
        boolean active
) {
}

