package com.dotani.shared.attachment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A file attached to an event, a post or a chat message. Owners map their attachments as a read-only
 * collection on owner_id filtered by owner_type; the owner id is set here when the attachment is created.
 */
@Entity
@Table(name = "attachments")
public class Attachment {

    @Id
    @Column(length = 36)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 20)
    private AttachmentOwnerType ownerType;

    @Column(name = "owner_id", nullable = false, length = 36)
    private String ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttachmentKind kind;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(length = 255)
    private String name;

    @Column(length = 120)
    private String mime;

    private Long size;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Attachment() {
    }

    public static Attachment of(
            AttachmentOwnerType ownerType,
            String ownerId,
            AttachmentKind kind,
            String url,
            String name,
            String mime,
            Long size,
            String createdBy
    ) {
        Attachment attachment = new Attachment();
        attachment.ownerType = ownerType;
        attachment.ownerId = ownerId;
        attachment.kind = kind;
        attachment.url = url;
        attachment.name = name;
        attachment.mime = mime;
        attachment.size = size;
        attachment.createdBy = createdBy;
        return attachment;
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public String getId() {
        return id;
    }

    public AttachmentOwnerType getOwnerType() {
        return ownerType;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public AttachmentKind getKind() {
        return kind;
    }

    public String getUrl() {
        return url;
    }

    public String getName() {
        return name;
    }

    public String getMime() {
        return mime;
    }

    public Long getSize() {
        return size;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
