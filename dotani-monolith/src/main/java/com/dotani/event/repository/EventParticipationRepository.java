package com.dotani.event.repository;

import com.dotani.event.entity.EventParticipation;
import com.dotani.event.entity.ParticipationStatus;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventParticipationRepository extends JpaRepository<EventParticipation, String> {

    Optional<EventParticipation> findByEvent_IdAndMemberId(String eventId, String memberId);

    List<EventParticipation> findByEvent_IdOrderByUpdatedAtDesc(String eventId);

    // excludes participations in deleted events
    List<EventParticipation> findByMemberIdAndEvent_DeletedAtIsNullOrderByUpdatedAtDesc(String memberId);

    long countByEvent_IdAndStatus(String eventId, ParticipationStatus status);

    long countByMemberIdAndStatus(String memberId, ParticipationStatus status);

    @Query("select participation.event.id, count(participation) from EventParticipation participation "
            + "where participation.event.id in :eventIds and participation.status = :status "
            + "group by participation.event.id")
    List<Object[]> countGroupedByEvent(
            @Param("eventIds") Collection<String> eventIds,
            @Param("status") ParticipationStatus status
    );

    /** GOING count per event id, in one query; events without registrations are absent (count 0). */
    default Map<String, Long> goingCounts(Collection<String> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        Map<String, Long> counts = new HashMap<>();
        for (Object[] row : countGroupedByEvent(eventIds, ParticipationStatus.GOING)) {
            counts.put((String) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    @Query("select count(participation) from EventParticipation participation "
            + "where participation.status = :status "
            + "and (:organizationId is null or participation.event.organizationId = :organizationId)")
    long countByStatusScoped(
            @Param("status") ParticipationStatus status,
            @Param("organizationId") String organizationId
    );
}
