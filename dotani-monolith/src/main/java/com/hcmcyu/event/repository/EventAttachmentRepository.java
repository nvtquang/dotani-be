package com.hcmcyu.event.repository;

import com.hcmcyu.event.entity.EventAttachment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventAttachmentRepository extends JpaRepository<EventAttachment, String> {

    Optional<EventAttachment> findByIdAndEvent_Id(String id, String eventId);
}
