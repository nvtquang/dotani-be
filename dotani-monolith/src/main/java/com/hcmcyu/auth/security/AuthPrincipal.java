package com.hcmcyu.auth.security;

import com.hcmcyu.auth.entity.Role;

public record AuthPrincipal(
        String userId,
        String memberId,
        String username,
        String email,
        Role role,
        String organizationId,
        String tdpId
) {
}

