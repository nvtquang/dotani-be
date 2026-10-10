package com.dotani.audit.dto;

import com.dotani.audit.entity.AuditAction;
import com.dotani.audit.entity.AuditResult;
import com.dotani.audit.entity.ResourceType;
import java.time.LocalDateTime;

public record AuditLogResponse(
        String id,
        String actorUserId,
        String actorRole,
        AuditAction action,
        ResourceType resourceType,
        String resourceId,
        String organizationId,
        LocalDateTime timestamp,
        AuditResult result
) {
}
