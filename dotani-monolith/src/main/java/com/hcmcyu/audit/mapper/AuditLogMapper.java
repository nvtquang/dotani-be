package com.hcmcyu.audit.mapper;

import com.hcmcyu.audit.dto.AuditLogCreateRequest;
import com.hcmcyu.audit.dto.AuditLogResponse;
import com.hcmcyu.audit.entity.AuditLog;
import org.springframework.stereotype.Component;

@Component
public class AuditLogMapper {

    public AuditLog toEntity(AuditLogCreateRequest request) {
        AuditLog auditLog = new AuditLog();
        auditLog.setActorUserId(request.actorUserId());
        auditLog.setActorRole(request.actorRole());
        auditLog.setAction(request.action());
        auditLog.setResourceType(request.resourceType());
        auditLog.setResourceId(request.resourceId());
        auditLog.setOrganizationId(request.organizationId());
        auditLog.setResult(request.result());
        return auditLog;
    }

    public AuditLogResponse toResponse(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getActorUserId(),
                auditLog.getActorRole(),
                auditLog.getAction(),
                auditLog.getResourceType(),
                auditLog.getResourceId(),
                auditLog.getOrganizationId(),
                auditLog.getTimestamp(),
                auditLog.getResult()
        );
    }
}
