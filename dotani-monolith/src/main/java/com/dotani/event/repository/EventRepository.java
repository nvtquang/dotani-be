package com.dotani.event.repository;

import com.dotani.event.entity.EventStatus;
import java.time.Instant;
import java.util.List;
import com.dotani.event.entity.Event;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, String>, JpaSpecificationExecutor<Event> {

    // published events whose registration closes in the given window and were not reminded yet
    List<Event> findByStatusAndRegistrationDeadlineBetweenAndDeadlineRemindedAtIsNull(EventStatus status, Instant from, Instant to);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select event from Event event where event.id = :id")
    Optional<Event> findByIdForUpdate(@Param("id") String id);
}
