package com.dotani.chat.mapper;

import com.dotani.shared.attachment.Attachment;
import com.dotani.chat.dto.ConversationResponse;
import com.dotani.chat.dto.MessageResponse;
import com.dotani.chat.entity.Conversation;
import com.dotani.chat.entity.Message;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ChatMapper {

    public ConversationResponse toResponse(Conversation conversation) {
        return toResponse(conversation, Map.of(), null, null, 0);
    }

    public ConversationResponse toResponse(Conversation conversation, Map<String, String> memberNames) {
        return toResponse(conversation, memberNames, null, null, 0);
    }

    public ConversationResponse toResponse(
            Conversation conversation,
            Map<String, String> memberNames,
            Instant lastMessageAt,
            String lastMessagePreview,
            long unreadCount
    ) {
        return new ConversationResponse(
                conversation.getId(),
                conversation.getType(),
                conversation.getTitle(),
                conversation.getAvatarUrl(),
                conversation.getCreatedBy(),
                conversation.getMembers().stream()
                        .map(member -> member.getMemberId())
                        .sorted()
                        .toList(),
                memberNames,
                lastMessageAt,
                lastMessagePreview,
                unreadCount,
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    public MessageResponse toResponse(Message message) {
        return toResponse(message, null);
    }

    public MessageResponse toResponse(Message message, String senderName) {
        Attachment attachment = message.getAttachment();
        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                message.getSenderId(),
                senderName,
                message.getContent(),
                attachment == null ? null : attachment.getUrl(),
                attachment == null ? null : attachment.getName(),
                attachment == null ? null : attachment.getMime(),
                attachment == null ? null : attachment.getSize(),
                attachment == null ? null : attachment.getKind(),
                message.getCreatedAt()
        );
    }
}
