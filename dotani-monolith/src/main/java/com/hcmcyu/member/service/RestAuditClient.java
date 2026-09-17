package com.hcmcyu.member.service;

import com.hcmcyu.member.entity.Member;
import com.hcmcyu.member.security.CurrentUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RestAuditClient implements AuditClient {

    private final com.hcmcyu.audit.service.AuditLogService auditLogService;
    private final String internalSecret;

    public RestAuditClient(
            com.hcmcyu.audit.service.AuditLogService auditLogService,
            @Value("${audit.internal.secret:dev-audit-internal-secret}") String internalSecret
    ) {
        this.auditLogService = auditLogService;
        this.internalSecret = internalSecret;
    }

    @Override
    public void record(AuditAction action, Member member, CurrentUser currentUser) {
        auditLogService.createInternal(new com.hcmcyu.audit.dto.AuditLogCreateRequest(
                currentUser.userId(),
                currentUser.role().name(),
                com.hcmcyu.audit.entity.AuditAction.valueOf(action.name()),
                com.hcmcyu.audit.entity.ResourceType.MEMBER,
                member.getId(),
                member.getOrganization().getId(),
                com.hcmcyu.audit.entity.AuditResult.SUCCESS
        ), internalSecret);
    }
}
