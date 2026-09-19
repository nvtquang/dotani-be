package com.hcmcyu.chat.dto;

import com.hcmcyu.chat.entity.ConversationType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ConversationResponse(
        String id,
        ConversationType type,
        String title,
        String avatarUrl,
        String createdBy,
        List<String> memberIds,
        Map<String, String> memberNames,
        LocalDateTime lastMessageAt,
        String lastMessagePreview,
        long unreadCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
