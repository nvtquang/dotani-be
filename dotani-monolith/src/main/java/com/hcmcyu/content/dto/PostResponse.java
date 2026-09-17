package com.hcmcyu.content.dto;

import com.hcmcyu.content.entity.PostStatus;
import com.hcmcyu.content.entity.PostType;
import java.time.LocalDateTime;
import java.util.List;

public record PostResponse(
        String id,
        String title,
        String content,
        PostType type,
        String organizationId,
        String authorId,
        PostStatus status,
        List<PostImageResponse> images,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
