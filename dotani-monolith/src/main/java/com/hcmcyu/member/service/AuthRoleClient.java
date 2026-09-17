package com.hcmcyu.member.service;

import com.hcmcyu.member.entity.MemberRole;

public interface AuthRoleClient {

    void updateUserRole(String userId, MemberRole role, String actorUserId, String memberId);
}
