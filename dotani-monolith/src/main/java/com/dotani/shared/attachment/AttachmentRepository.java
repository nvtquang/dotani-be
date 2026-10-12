package com.dotani.shared.attachment;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<Attachment, String> {

    Optional<Attachment> findByIdAndOwnerTypeAndOwnerId(String id, AttachmentOwnerType ownerType, String ownerId);
}
