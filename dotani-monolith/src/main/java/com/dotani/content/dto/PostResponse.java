package com.dotani.content.dto;

import com.dotani.content.entity.PostStatus;
import com.dotani.content.entity.PostType;
import java.time.Instant;
import java.util.List;

public record PostResponse(
        String id,
        String title,
        String content,
        PostType type,
        String organizationId,
        String organizationName,
        String authorId,
        String authorName,
        PostStatus status,
        List<PostImageResponse> images,
        Instant createdAt,
        Instant updatedAt
) {
}
