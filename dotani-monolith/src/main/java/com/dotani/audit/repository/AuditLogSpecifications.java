package com.dotani.audit.repository;

import com.dotani.shared.time.AppTime;
import com.dotani.audit.entity.AuditAction;
import com.dotani.audit.entity.AuditLog;
import com.dotani.audit.entity.AuditResult;
import com.dotani.audit.entity.ResourceType;
import java.time.LocalDate;
import java.time.Instant;
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
            Instant start = AppTime.startOfDay(date);
            Instant end = AppTime.startOfNextDay(date);
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
