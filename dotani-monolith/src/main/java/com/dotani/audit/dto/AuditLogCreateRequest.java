package com.dotani.audit.dto;

import com.dotani.audit.entity.AuditAction;
import com.dotani.audit.entity.AuditResult;
import com.dotani.audit.entity.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AuditLogCreateRequest(
        @NotBlank @Size(max = 36) String actorUserId,
        @NotBlank @Size(max = 50) String actorRole,
        @NotNull AuditAction action,
        @NotNull ResourceType resourceType,
        @NotBlank @Size(max = 36) String resourceId,
        @Size(max = 36) String organizationId,
        @NotNull AuditResult result
) {
}
