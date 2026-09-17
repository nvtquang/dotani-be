package com.hcmcyu.member.service;

import com.hcmcyu.member.dto.AuthRoleUpdateRequest;
import com.hcmcyu.member.entity.MemberRole;
import com.hcmcyu.auth.dto.InternalRoleUpdateRequest;
import com.hcmcyu.auth.service.InternalUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RestAuthRoleClient implements AuthRoleClient {

    private final InternalUserService internalUserService;
    private final String internalSecret;

    public RestAuthRoleClient(
            InternalUserService internalUserService,
            @Value("${services.auth.internal-secret:dev-internal-secret}") String internalSecret
    ) {
        this.internalUserService = internalUserService;
        this.internalSecret = internalSecret;
    }

    @Override
    public void updateUserRole(String userId, MemberRole role, String actorUserId, String memberId) {
        AuthRoleUpdateRequest request = new AuthRoleUpdateRequest(role, actorUserId, memberId);
        internalUserService.updateRole(
                userId,
                new InternalRoleUpdateRequest(
                        com.hcmcyu.auth.entity.Role.valueOf(request.role().name()),
                        request.actorUserId(),
                        request.memberId()
                ),
                internalSecret
        );
    }
}
