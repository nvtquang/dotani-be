package com.dotani.event.repository;

import com.dotani.shared.time.AppTime;
import com.dotani.event.entity.Event;
import com.dotani.event.entity.EventStatus;
import com.dotani.event.entity.EventType;
import com.dotani.shared.security.CurrentUser;
import com.dotani.shared.security.OrgScopePolicy;
import java.time.LocalDate;
import java.time.Instant;
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
            Instant start = AppTime.startOfDay(date);
            Instant end = AppTime.startOfNextDay(date);
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
            return criteriaBuilder.greaterThanOrEqualTo(root.get("startTime"), Instant.now());
        };
    }

    public static Specification<Event> withinReadScope(CurrentUser currentUser) {
        return (root, query, criteriaBuilder) ->
                OrgScopePolicy.contentVisibleTo(currentUser, criteriaBuilder, root.get("organizationId"));
    }
}
