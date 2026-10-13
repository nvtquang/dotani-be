package com.dotani.member.repository;

import com.dotani.member.entity.MemberTransfer;
import com.dotani.member.entity.TransferStatus;
import com.dotani.shared.security.CurrentUser;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MemberTransferRepository extends JpaRepository<MemberTransfer, String>, JpaSpecificationExecutor<MemberTransfer> {

    boolean existsByMemberIdAndStatus(String memberId, TransferStatus status);

    /** Ward officers see every transfer, TDP officers those leaving or entering their TDP, members their own. */
    static Specification<MemberTransfer> visibleTo(CurrentUser user) {
        return (root, query, cb) -> {
            if (user.hasWardScope()) {
                return cb.conjunction();
            }
            if (user.hasTdpScope() && user.tdpId() != null) {
                return cb.or(
                        cb.equal(root.get("fromOrganizationId"), user.tdpId()),
                        cb.equal(root.get("toOrganizationId"), user.tdpId()));
            }
            return user.memberId() == null ? cb.disjunction() : cb.equal(root.get("memberId"), user.memberId());
        };
    }

    static Specification<MemberTransfer> hasStatus(TransferStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    static Specification<MemberTransfer> ofMember(String memberId) {
        return (root, query, cb) -> memberId == null || memberId.isBlank() ? cb.conjunction() : cb.equal(root.get("memberId"), memberId);
    }
}
