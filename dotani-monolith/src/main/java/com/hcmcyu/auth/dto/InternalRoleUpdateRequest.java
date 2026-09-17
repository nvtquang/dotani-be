package com.hcmcyu.auth.dto;

import com.hcmcyu.auth.entity.Role;
import jakarta.validation.constraints.NotNull;

public record InternalRoleUpdateRequest(
        @NotNull
        Role role,

        String actorUserId,

        String memberId
) {
}
