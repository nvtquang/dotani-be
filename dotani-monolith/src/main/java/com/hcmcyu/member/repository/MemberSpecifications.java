package com.hcmcyu.member.repository;

import com.hcmcyu.member.entity.Member;
import com.hcmcyu.member.entity.MemberRole;
import com.hcmcyu.member.entity.MemberStatus;
import com.hcmcyu.member.security.CurrentUser;
import org.springframework.data.jpa.domain.Specification;

public final class MemberSpecifications {

    private MemberSpecifications() {
    }

    public static Specification<Member> keywordContains(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (keyword == null || keyword.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("fullName")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("phone")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("address")), pattern)
            );
        };
    }

    public static Specification<Member> hasOrganization(String organizationId) {
        return (root, query, criteriaBuilder) -> {
            if (organizationId == null || organizationId.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("organization").get("id"), organizationId);
        };
    }

    public static Specification<Member> hasStatus(MemberStatus status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("memberStatus"), status);
        };
    }

    public static Specification<Member> hasRole(MemberRole role) {
        return (root, query, criteriaBuilder) -> {
            if (role == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("memberRole"), role);
        };
    }

    public static Specification<Member> withinScope(CurrentUser currentUser) {
        return (root, query, criteriaBuilder) -> {
            if (currentUser.hasWardScope()) {
                return criteriaBuilder.conjunction();
            }
            if (currentUser.hasTdpScope()) {
                return criteriaBuilder.equal(root.get("organization").get("id"), currentUser.tdpId());
            }
            if (currentUser.isMember() && currentUser.memberId() != null) {
                return criteriaBuilder.equal(root.get("id"), currentUser.memberId());
            }
            return criteriaBuilder.disjunction();
        };
    }
}
