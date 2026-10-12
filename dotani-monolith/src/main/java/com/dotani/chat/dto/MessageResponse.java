package com.dotani.chat.dto;

import com.dotani.shared.attachment.AttachmentKind;
import java.time.Instant;

public record MessageResponse(
        String id,
        String conversationId,
        String senderId,
        String senderName,
        String content,
        String attachmentUrl,
        String attachmentName,
        String attachmentContentType,
        Long attachmentSize,
        AttachmentKind attachmentKind,
        Instant createdAt
) {
}
