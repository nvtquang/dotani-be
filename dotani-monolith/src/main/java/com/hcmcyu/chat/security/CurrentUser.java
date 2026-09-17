package com.hcmcyu.chat.security;

public record CurrentUser(
        String userId,
        String memberId,
        String username,
        String email,
        Role role,
        String organizationId,
        String tdpId
) {
}
