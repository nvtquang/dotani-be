package com.hcmcyu.event.repository;

import com.hcmcyu.event.entity.EventParticipation;
import com.hcmcyu.event.entity.ParticipationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventParticipationRepository extends JpaRepository<EventParticipation, String> {

    Optional<EventParticipation> findByEvent_IdAndMemberId(String eventId, String memberId);

    List<EventParticipation> findByEvent_IdOrderByUpdatedAtDesc(String eventId);

    List<EventParticipation> findByMemberIdOrderByUpdatedAtDesc(String memberId);

    long countByEvent_IdAndStatus(String eventId, ParticipationStatus status);

    long countByMemberIdAndStatus(String memberId, ParticipationStatus status);

    @Query("select count(participation) from EventParticipation participation "
            + "where participation.status = :status "
            + "and (:organizationId is null or participation.event.organizationId = :organizationId)")
    long countByStatusScoped(
            @Param("status") ParticipationStatus status,
            @Param("organizationId") String organizationId
    );
}
