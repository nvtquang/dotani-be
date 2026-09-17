package com.hcmcyu.audit.service;

import com.hcmcyu.audit.dto.AuditLogCreateRequest;
import com.hcmcyu.audit.dto.AuditLogResponse;
import com.hcmcyu.audit.entity.AuditAction;
import com.hcmcyu.audit.entity.AuditResult;
import com.hcmcyu.audit.entity.ResourceType;
import com.hcmcyu.audit.exception.AuditServiceException;
import com.hcmcyu.audit.mapper.AuditLogMapper;
import com.hcmcyu.audit.repository.AuditLogRepository;
import com.hcmcyu.audit.repository.AuditLogSpecifications;
import com.hcmcyu.audit.security.CurrentUser;
import com.hcmcyu.audit.security.Role;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
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
    private final String internalSecret;

    public AuditLogService(
            AuditLogRepository auditLogRepository,
            AuditLogMapper auditLogMapper,
            @Value("${audit.internal.secret}") String internalSecret
    ) {
        this.auditLogRepository = auditLogRepository;
        this.auditLogMapper = auditLogMapper;
        this.internalSecret = internalSecret;
    }

    @Transactional
    public AuditLogResponse createInternal(AuditLogCreateRequest request, String providedSecret) {
        if (providedSecret == null || !providedSecret.equals(internalSecret)) {
            throw new AuditServiceException(
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_INTERNAL_SECRET",
                    "Internal audit secret is invalid"
            );
        }
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
        Specification<com.hcmcyu.audit.entity.AuditLog> specification = Specification
                .where(AuditLogSpecifications.hasAction(action))
                .and(AuditLogSpecifications.hasResourceType(resourceType))
                .and(AuditLogSpecifications.hasOrganization(organizationId))
                .and(AuditLogSpecifications.hasResult(result))
                .and(AuditLogSpecifications.onDate(date));

        if (currentUser.role() == Role.WARD_DEPUTY_SECRETARY) {
            specification = specification.and(AuditLogSpecifications.hidesRoleChanges());
        } else if (currentUser.role() != Role.WARD_SECRETARY) {
            throw new AuditServiceException(
                    HttpStatus.FORBIDDEN,
                    "AUDIT_LOG_FORBIDDEN",
                    "Only ward-level officers can view audit logs"
            );
        }

        return auditLogRepository.findAll(specification, pageable)
                .map(auditLogMapper::toResponse);
    }
}
