package com.hcmcyu.audit.repository;

import com.hcmcyu.audit.entity.AuditAction;
import com.hcmcyu.audit.entity.AuditLog;
import com.hcmcyu.audit.entity.AuditResult;
import com.hcmcyu.audit.entity.ResourceType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.Specification;

public final class AuditLogSpecifications {

    private AuditLogSpecifications() {
    }

    public static Specification<AuditLog> hasAction(AuditAction action) {
        return (root, query, criteriaBuilder) -> action == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get("action"), action);
    }

    public static Specification<AuditLog> hasResourceType(ResourceType resourceType) {
        return (root, query, criteriaBuilder) -> resourceType == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get("resourceType"), resourceType);
    }

    public static Specification<AuditLog> hasOrganization(String organizationId) {
        return (root, query, criteriaBuilder) -> {
            if (organizationId == null || organizationId.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("organizationId"), organizationId);
        };
    }

    public static Specification<AuditLog> hasResult(AuditResult result) {
        return (root, query, criteriaBuilder) -> result == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get("result"), result);
    }

    public static Specification<AuditLog> onDate(LocalDate date) {
        return (root, query, criteriaBuilder) -> {
            if (date == null) {
                return criteriaBuilder.conjunction();
            }
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            return criteriaBuilder.and(
                    criteriaBuilder.greaterThanOrEqualTo(root.get("timestamp"), start),
                    criteriaBuilder.lessThan(root.get("timestamp"), end)
            );
        };
    }

    public static Specification<AuditLog> hidesRoleChanges() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.notEqual(root.get("action"), AuditAction.CHANGE_ROLE);
    }
}
