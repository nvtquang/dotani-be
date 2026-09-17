package com.hcmcyu.content.dto;

import java.time.LocalDateTime;

public record PostImageResponse(
        String id,
        String imageUrl,
        LocalDateTime createdAt
) {
}
