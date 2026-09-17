package com.hcmcyu.event.security;

public record CurrentUser(
        String userId,
        String memberId,
        String username,
        Role role,
        String organizationId,
        String tdpId
) {

    public boolean hasWardScope() {
        return role == Role.WARD_SECRETARY || role == Role.WARD_DEPUTY_SECRETARY;
    }

    public boolean hasTdpScope() {
        return role == Role.TDP_SECRETARY || role == Role.TDP_DEPUTY_SECRETARY;
    }

    public boolean isMember() {
        return role == Role.MEMBER;
    }
}
