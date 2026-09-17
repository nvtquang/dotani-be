package com.hcmcyu.chat.dto;

import com.hcmcyu.chat.entity.AttachmentKind;
import java.time.LocalDateTime;

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
        LocalDateTime createdAt
) {
}
