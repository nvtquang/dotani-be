package com.dotani.poll.repository;

import com.dotani.poll.entity.Poll;
import com.dotani.poll.entity.PollStatus;
import com.dotani.shared.security.CurrentUser;
import com.dotani.shared.security.OrgScopePolicy;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PollRepository extends JpaRepository<Poll, String>, JpaSpecificationExecutor<Poll> {

    List<Poll> findByStatusAndClosesAtLessThanEqual(PollStatus status, Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select poll from Poll poll where poll.id = :id")
    Optional<Poll> findByIdForUpdate(@Param("id") String id);

    /** Polls of the caller's ward and own TDP; members never see drafts. */
    static Specification<Poll> visibleTo(CurrentUser user) {
        return (root, query, cb) -> {
            var inScope = OrgScopePolicy.contentVisibleTo(user, cb, root.get("organizationId"));
            return user.isMember() ? cb.and(inScope, cb.notEqual(root.get("status"), PollStatus.DRAFT)) : inScope;
        };
    }

    static Specification<Poll> hasStatus(PollStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    static Specification<Poll> ofEvent(String eventId) {
        return (root, query, cb) -> eventId == null || eventId.isBlank() ? cb.conjunction() : cb.equal(root.get("eventId"), eventId);
    }
}
