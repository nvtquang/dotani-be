package com.dotani.chat.dto;

import com.dotani.chat.entity.ConversationType;
import java.time.Instant;
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
        Instant lastMessageAt,
        String lastMessagePreview,
        long unreadCount,
        // when each member last read the conversation (null = never); drives "seen" marks
        Map<String, Instant> readStates,
        Instant createdAt,
        Instant updatedAt
) {
}
