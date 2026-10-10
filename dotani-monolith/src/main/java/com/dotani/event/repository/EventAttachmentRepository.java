package com.dotani.event.repository;

import com.dotani.event.entity.EventAttachment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventAttachmentRepository extends JpaRepository<EventAttachment, String> {

    Optional<EventAttachment> findByIdAndEvent_Id(String id, String eventId);
}
