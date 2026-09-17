package com.hcmcyu.content.service;

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
