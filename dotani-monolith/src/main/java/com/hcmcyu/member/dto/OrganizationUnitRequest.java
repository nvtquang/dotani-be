package com.hcmcyu.member.dto;

import com.hcmcyu.member.entity.OrganizationUnitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrganizationUnitRequest(
        @NotBlank
        @Size(max = 255)
        String name,

        @NotBlank
        @Size(max = 100)
        String code,

        @NotNull
        OrganizationUnitType type,

        String parentId,

        Boolean active
) {
}

