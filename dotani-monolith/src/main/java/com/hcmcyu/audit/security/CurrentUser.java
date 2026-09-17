package com.hcmcyu.audit.security;

public record CurrentUser(
        String userId,
        String memberId,
        String username,
        Role role,
        String organizationId,
        String tdpId
) {
}
