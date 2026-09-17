package com.hcmcyu.member.service;

public record AuditLogRequest(
        String actorUserId,
        String actorRole,
        AuditAction action,
        AuditResourceType resourceType,
        String resourceId,
        String organizationId,
        AuditResult result
) {
}
