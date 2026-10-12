package com.dotani.audit.dto;

import java.util.Map;
import com.dotani.audit.entity.AuditAction;
import com.dotani.audit.entity.AuditResult;
import com.dotani.audit.entity.ResourceType;
import java.time.Instant;

public record AuditLogResponse(
        String id,
        String actorUserId,
        String actorRole,
        AuditAction action,
        ResourceType resourceType,
        String resourceId,
        String organizationId,
        Instant timestamp,
        AuditResult result,
        Map<String, Object> details,
        String ip,
        String userAgent
) {
}
