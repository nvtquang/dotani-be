package com.dotani.shared.security;

import com.dotani.shared.exception.ApiException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;

/**
 * Organization-scope rules shared by every module. A caller is scoped to a ward
 * ({@link CurrentUser#organizationId()}) and, below ward level, to one TDP ({@link CurrentUser#tdpId()}).
 * Module-specific rules (published-only posts, protecting ward officers, ...) stay in each module and
 * build on these primitives.
 */
public final class OrgScopePolicy {

    private OrgScopePolicy() {
    }

    /** Ward officers act on every organization in the ward. */
    public static boolean coversWholeWard(CurrentUser user) {
        return user.hasWardScope();
    }

    public static boolean isOwnTdp(CurrentUser user, String organizationId) {
        return organizationId != null && user.tdpId() != null && user.tdpId().equals(organizationId);
    }

    /** Content (events, posts) owned by the caller's ward or by the caller's own TDP. */
    public static boolean canSeeContentOf(CurrentUser user, String organizationId) {
        if (coversWholeWard(user)) {
            return true;
        }
        return organizationId != null
                && (organizationId.equals(user.organizationId()) || isOwnTdp(user, organizationId));
    }

    /**
     * Query form of {@link #canSeeContentOf}: restricts {@code organizationIdPath} to what the caller can see.
     * Ward officers are unrestricted; a caller with no scope sees nothing.
     */
    public static Predicate contentVisibleTo(CurrentUser user, CriteriaBuilder cb, Expression<String> organizationIdPath) {
        if (coversWholeWard(user)) {
            return cb.conjunction();
        }
        List<Predicate> predicates = new ArrayList<>();
        if (hasText(user.organizationId())) {
            predicates.add(cb.equal(organizationIdPath, user.organizationId()));
        }
        if (hasText(user.tdpId())) {
            predicates.add(cb.equal(organizationIdPath, user.tdpId()));
        }
        return predicates.isEmpty() ? cb.disjunction() : cb.or(predicates.toArray(Predicate[]::new));
    }

    public static ApiException outOfScope(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, "OUT_OF_SCOPE", message);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
