package com.hcmcyu.member.dto;

import com.hcmcyu.member.entity.MemberRole;
import jakarta.validation.constraints.NotNull;

public record MemberRoleUpdateRequest(
        @NotNull
        MemberRole role
) {
}
