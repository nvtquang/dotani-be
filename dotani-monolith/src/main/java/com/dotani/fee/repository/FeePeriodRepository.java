package com.dotani.fee.repository;

import com.dotani.fee.entity.FeePeriod;
import com.dotani.fee.entity.FeePeriodStatus;
import com.dotani.shared.security.CurrentUser;
import com.dotani.shared.security.OrgScopePolicy;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FeePeriodRepository extends JpaRepository<FeePeriod, String>, JpaSpecificationExecutor<FeePeriod> {

    /** Periods of the caller's ward and of the caller's own TDP (everything for ward officers). */
    static Specification<FeePeriod> visibleTo(CurrentUser user) {
        return (root, query, cb) -> OrgScopePolicy.contentVisibleTo(user, cb, root.get("organizationId"));
    }

    static Specification<FeePeriod> hasStatus(FeePeriodStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }
}
