package com.hcmcyu.auth.dto;

import com.hcmcyu.auth.entity.Role;

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

