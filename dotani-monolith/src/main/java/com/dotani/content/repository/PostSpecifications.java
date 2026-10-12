package com.dotani.content.repository;

import com.dotani.shared.time.AppTime;
import com.dotani.content.entity.Post;
import com.dotani.content.entity.PostStatus;
import com.dotani.content.entity.PostType;
import com.dotani.shared.security.CurrentUser;
import com.dotani.shared.security.OrgScopePolicy;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.Instant;
import org.springframework.data.jpa.domain.Specification;

public final class PostSpecifications {

    private PostSpecifications() {
    }

    public static Specification<Post> hasOrganization(String organizationId) {
        return (root, query, criteriaBuilder) -> {
            if (organizationId == null || organizationId.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("organizationId"), organizationId);
        };
    }

    public static Specification<Post> hasType(PostType type) {
        return (root, query, criteriaBuilder) -> type == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get("type"), type);
    }

    public static Specification<Post> onDate(LocalDate date) {
        return (root, query, criteriaBuilder) -> {
            if (date == null) {
                return criteriaBuilder.conjunction();
            }
            Instant start = AppTime.startOfDay(date);
            Instant end = AppTime.startOfNextDay(date);
            return criteriaBuilder.and(
                    criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), start),
                    criteriaBuilder.lessThan(root.get("createdAt"), end)
            );
        };
    }

    public static Specification<Post> createdAfter(Instant createdAfter) {
        return (root, query, criteriaBuilder) -> createdAfter == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdAfter);
    }

    public static Specification<Post> visibleTo(CurrentUser currentUser) {
        return (root, query, criteriaBuilder) -> {
            if (query != null) {
                query.distinct(true);
            }
            if (currentUser.hasWardScope()) {
                return criteriaBuilder.conjunction();
            }

            Predicate scope = OrgScopePolicy.contentVisibleTo(currentUser, criteriaBuilder, root.get("organizationId"));
            if (currentUser.isMember()) {
                return criteriaBuilder.and(scope, criteriaBuilder.equal(root.get("status"), PostStatus.PUBLISHED));
            }
            return scope;
        };
    }
}
