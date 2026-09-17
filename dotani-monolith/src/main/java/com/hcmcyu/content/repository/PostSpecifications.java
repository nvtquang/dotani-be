package com.hcmcyu.content.repository;

import com.hcmcyu.content.entity.Post;
import com.hcmcyu.content.entity.PostStatus;
import com.hcmcyu.content.entity.PostType;
import com.hcmcyu.content.security.CurrentUser;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            return criteriaBuilder.and(
                    criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), start),
                    criteriaBuilder.lessThan(root.get("createdAt"), end)
            );
        };
    }

    public static Specification<Post> createdAfter(LocalDateTime createdAfter) {
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

            List<Predicate> scopePredicates = new ArrayList<>();
            if (currentUser.organizationId() != null && !currentUser.organizationId().isBlank()) {
                scopePredicates.add(criteriaBuilder.equal(root.get("organizationId"), currentUser.organizationId()));
            }
            if (currentUser.tdpId() != null && !currentUser.tdpId().isBlank()) {
                scopePredicates.add(criteriaBuilder.equal(root.get("organizationId"), currentUser.tdpId()));
            }
            if (scopePredicates.isEmpty()) {
                return criteriaBuilder.disjunction();
            }

            Predicate scope = criteriaBuilder.or(scopePredicates.toArray(Predicate[]::new));
            if (currentUser.isMember()) {
                return criteriaBuilder.and(scope, criteriaBuilder.equal(root.get("status"), PostStatus.PUBLISHED));
            }
            return scope;
        };
    }
}
