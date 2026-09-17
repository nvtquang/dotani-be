package com.hcmcyu.audit.dto;

import com.hcmcyu.audit.entity.AuditAction;
import com.hcmcyu.audit.entity.AuditResult;
import com.hcmcyu.audit.entity.ResourceType;
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
