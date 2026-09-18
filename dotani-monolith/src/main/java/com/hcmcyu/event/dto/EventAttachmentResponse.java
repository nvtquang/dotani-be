package com.hcmcyu.event.dto;

import java.time.LocalDateTime;

public record EventAttachmentResponse(
        String id,
        String fileUrl,
        String attachmentKind,
        String fileName,
        LocalDateTime createdAt
) {
}
