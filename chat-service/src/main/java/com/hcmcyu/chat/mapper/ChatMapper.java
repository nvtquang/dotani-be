package com.hcmcyu.chat.mapper;

import com.hcmcyu.chat.dto.ConversationResponse;
import com.hcmcyu.chat.dto.MessageResponse;
import com.hcmcyu.chat.entity.Conversation;
import com.hcmcyu.chat.entity.Message;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ChatMapper {

    public ConversationResponse toResponse(Conversation conversation) {
        return toResponse(conversation, Map.of());
    }

    public ConversationResponse toResponse(Conversation conversation, Map<String, String> memberNames) {
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
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    public MessageResponse toResponse(Message message) {
        return toResponse(message, null);
    }

    public MessageResponse toResponse(Message message, String senderName) {
        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                message.getSenderId(),
                senderName,
                message.getContent(),
                message.getAttachmentUrl(),
                message.getAttachmentName(),
                message.getAttachmentContentType(),
                message.getAttachmentSize(),
                message.getAttachmentKind(),
                message.getCreatedAt()
        );
    }
}
