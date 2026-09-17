package com.hcmcyu.member.dto;

import com.hcmcyu.member.entity.MemberRole;

public record AuthRoleUpdateRequest(
        MemberRole role,
        String actorUserId,
        String memberId
) {
}
