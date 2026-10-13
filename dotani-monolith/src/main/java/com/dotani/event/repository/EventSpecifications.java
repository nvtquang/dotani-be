package com.dotani.event.repository;

import com.dotani.shared.time.AppTime;
import com.dotani.event.dto.EventFilter;
import com.dotani.event.entity.Event;
import com.dotani.event.entity.EventParticipation;
import com.dotani.event.entity.ParticipationStatus;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import com.dotani.event.entity.EventStatus;
import com.dotani.event.entity.EventType;
import com.dotani.shared.persistence.TextSearch;
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

    public static Specification<Event> when(EventFilter.When when) {
        return (root, query, criteriaBuilder) -> {
            if (when == null) {
                return criteriaBuilder.conjunction();
            }
            Instant now = Instant.now();
            return when == EventFilter.When.UPCOMING
                    ? criteriaBuilder.greaterThanOrEqualTo(root.get("startTime"), now)
                    : criteriaBuilder.lessThan(root.get("startTime"), now);
        };
    }

    /** startTime in [from, to); either bound may be null. */
    public static Specification<Event> startsBetween(Instant from, Instant to) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.and(
                from == null ? criteriaBuilder.conjunction() : criteriaBuilder.greaterThanOrEqualTo(root.get("startTime"), from),
                to == null ? criteriaBuilder.conjunction() : criteriaBuilder.lessThan(root.get("startTime"), to)
        );
    }

    /** Events the member registered GOING for. */
    public static Specification<Event> participatedBy(String memberId) {
        return (root, query, criteriaBuilder) -> {
            Subquery<String> going = query.subquery(String.class);
            Root<EventParticipation> participation = going.from(EventParticipation.class);
            going.select(participation.get("event").get("id")).where(
                    criteriaBuilder.equal(participation.get("memberId"), memberId),
                    criteriaBuilder.equal(participation.get("status"), ParticipationStatus.GOING)
            );
            return root.get("id").in(going);
        };
    }

    /** Organization scope; plain members never see drafts (same rule as posts). */
    public static Specification<Event> withinReadScope(CurrentUser currentUser) {
        return (root, query, criteriaBuilder) -> {
            var scope = OrgScopePolicy.contentVisibleTo(currentUser, criteriaBuilder, root.get("organizationId"));
            if (currentUser.isMember()) {
                return criteriaBuilder.and(scope, criteriaBuilder.notEqual(root.get("status"), EventStatus.DRAFT));
            }
            return scope;
        };
    }

    public static Specification<Event> createdBefore(Instant before) {
        return (root, query, criteriaBuilder) -> before == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.lessThan(root.get("createdAt"), before);
    }

    public static Specification<Event> titleContains(String text) {
        return (root, query, criteriaBuilder) -> TextSearch.contains(criteriaBuilder, root.get("title"), text);
    }
}
