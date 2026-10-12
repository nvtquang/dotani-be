package com.dotani.content.dto;

import java.time.Instant;

public record PostImageResponse(
        String id,
        String imageUrl,
        String attachmentKind,
        String fileName,
        Instant createdAt
) {
}
