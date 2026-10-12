package com.dotani.event.dto;

import java.time.Instant;

public record EventAttachmentResponse(
        String id,
        String fileUrl,
        String attachmentKind,
        String fileName,
        Instant createdAt
) {
}
