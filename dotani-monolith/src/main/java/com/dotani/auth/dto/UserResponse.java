package com.dotani.auth.dto;

import com.dotani.shared.security.Role;

public record UserResponse(
        String userId,
        String memberId,
        String username,
        String email,
        Role role,
        String organizationId,
        String tdpId
) {
}

