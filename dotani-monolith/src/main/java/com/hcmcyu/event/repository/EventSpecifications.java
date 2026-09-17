package com.hcmcyu.event.repository;

import com.hcmcyu.event.entity.Event;
import com.hcmcyu.event.entity.EventStatus;
import com.hcmcyu.event.entity.EventType;
import com.hcmcyu.event.security.CurrentUser;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class EventSpecifications {

    private EventSpecifications() {
    }

    public static Specification<Event> hasType(EventType type) {
        return (root, query, criteriaBuilder) -> type == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get("type"), type);
    }

    public static Specification<Event> hasOrganization(String organizationId) {
        return (root, query, criteriaBuilder) -> {
            if (organizationId == null || organizationId.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("organizationId"), organizationId);
        };
    }

    public static Specification<Event> hasStatus(EventStatus status) {
        return (root, query, criteriaBuilder) -> status == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<Event> onDate(LocalDate date) {
        return (root, query, criteriaBuilder) -> {
            if (date == null) {
                return criteriaBuilder.conjunction();
            }
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            return criteriaBuilder.and(
                    criteriaBuilder.greaterThanOrEqualTo(root.get("startTime"), start),
                    criteriaBuilder.lessThan(root.get("startTime"), end)
            );
        };
    }

    public static Specification<Event> upcoming(Boolean upcoming) {
        return (root, query, criteriaBuilder) -> {
            if (upcoming == null || !upcoming) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("startTime"), LocalDateTime.now());
        };
    }

    public static Specification<Event> withinReadScope(CurrentUser currentUser) {
        return (root, query, criteriaBuilder) -> {
            if (currentUser.hasWardScope()) {
                return criteriaBuilder.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();
            if (currentUser.organizationId() != null && !currentUser.organizationId().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("organizationId"), currentUser.organizationId()));
            }
            if (currentUser.tdpId() != null && !currentUser.tdpId().isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("organizationId"), currentUser.tdpId()));
            }
            if (predicates.isEmpty()) {
                return criteriaBuilder.disjunction();
            }
            return criteriaBuilder.or(predicates.toArray(Predicate[]::new));
        };
    }
}
