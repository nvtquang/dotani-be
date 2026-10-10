package com.dotani.member.dto;

import com.dotani.member.entity.MemberRole;
import jakarta.validation.constraints.NotNull;

public record MemberRoleUpdateRequest(
        @NotNull
        MemberRole role
) {
}
