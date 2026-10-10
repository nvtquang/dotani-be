package com.dotani.audit.service;

import com.dotani.audit.dto.AuditLogCreateRequest;
import com.dotani.audit.dto.AuditLogResponse;
import com.dotani.audit.entity.AuditAction;
import com.dotani.audit.entity.AuditResult;
import com.dotani.audit.entity.ResourceType;
import com.dotani.shared.exception.ApiException;
import com.dotani.audit.mapper.AuditLogMapper;
import com.dotani.audit.repository.AuditLogRepository;
import com.dotani.audit.repository.AuditLogSpecifications;
import com.dotani.shared.security.CurrentUser;
import com.dotani.shared.security.Role;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    public AuditLogService(
            AuditLogRepository auditLogRepository,
            AuditLogMapper auditLogMapper
    ) {
        this.auditLogRepository = auditLogRepository;
        this.auditLogMapper = auditLogMapper;
    }

    @Transactional
    public AuditLogResponse record(
            CurrentUser actor,
            AuditAction action,
            ResourceType resourceType,
            String resourceId,
            String organizationId
    ) {
        AuditLogCreateRequest request = new AuditLogCreateRequest(
                actor.userId(),
                actor.role().name(),
                action,
                resourceType,
                resourceId,
                organizationId,
                AuditResult.SUCCESS
        );
        return auditLogMapper.toResponse(auditLogRepository.save(auditLogMapper.toEntity(request)));
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> findAll(
            AuditAction action,
            ResourceType resourceType,
            String organizationId,
            AuditResult result,
            LocalDate date,
            Pageable pageable,
            CurrentUser currentUser
    ) {
        Specification<com.dotani.audit.entity.AuditLog> specification = Specification
                .where(AuditLogSpecifications.hasAction(action))
                .and(AuditLogSpecifications.hasResourceType(resourceType))
                .and(AuditLogSpecifications.hasOrganization(organizationId))
                .and(AuditLogSpecifications.hasResult(result))
                .and(AuditLogSpecifications.onDate(date));

        if (currentUser.role() == Role.WARD_DEPUTY_SECRETARY) {
            specification = specification.and(AuditLogSpecifications.hidesRoleChanges());
        } else if (currentUser.role() != Role.WARD_SECRETARY) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "AUDIT_LOG_FORBIDDEN",
                    "Only ward-level officers can view audit logs"
            );
        }

        return auditLogRepository.findAll(specification, pageable)
                .map(auditLogMapper::toResponse);
    }
}
