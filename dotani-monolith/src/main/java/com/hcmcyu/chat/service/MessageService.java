package com.hcmcyu.chat.service;

import com.hcmcyu.chat.dto.ChatMessageRequest;
import com.hcmcyu.chat.dto.MessageResponse;
import com.hcmcyu.chat.entity.Conversation;
import com.hcmcyu.chat.entity.Message;
import com.hcmcyu.chat.exception.ChatServiceException;
import com.hcmcyu.chat.mapper.ChatMapper;
import com.hcmcyu.chat.repository.ConversationRepository;
import com.hcmcyu.chat.repository.MessageRepository;
import com.hcmcyu.chat.security.CurrentUser;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ChatAuthorizationService authorizationService;
    private final ChatMapper chatMapper;
    private final MemberDirectoryClient memberDirectoryClient;
    private final ChatStorageService storageService;

    public MessageService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            ChatAuthorizationService authorizationService,
            ChatMapper chatMapper,
            MemberDirectoryClient memberDirectoryClient,
            ChatStorageService storageService
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.authorizationService = authorizationService;
        this.chatMapper = chatMapper;
        this.memberDirectoryClient = memberDirectoryClient;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public Page<MessageResponse> findMessages(String conversationId, Pageable pageable, CurrentUser currentUser) {
        authorizationService.requireConversationMember(conversationId, currentUser);
        Page<Message> messages = messageRepository.findByConversation_Id(conversationId, pageable);
        Map<String, String> senderNames = memberDirectoryClient.findDisplayNames(messages.getContent().stream()
                .map(Message::getSenderId)
                .toList());
        return messages.map(message -> chatMapper.toResponse(message, senderNames.get(message.getSenderId())));
    }

    @Transactional
    public MessageResponse sendMessage(String conversationId, ChatMessageRequest request, CurrentUser currentUser) {
        String senderId = authorizationService.requireMemberContext(currentUser);
        authorizationService.requireConversationMember(conversationId, currentUser);
        String content = validateContent(request);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ChatServiceException(
                        HttpStatus.NOT_FOUND,
                        "CONVERSATION_NOT_FOUND",
                        "Conversation not found"
                ));

        Message message = new Message();
        message.setConversation(conversation);
        message.setSenderId(senderId);
        message.setContent(content);
        Message saved = messageRepository.save(message);
        Map<String, String> senderNames = memberDirectoryClient.findDisplayNames(List.of(senderId));
        return chatMapper.toResponse(saved, senderNames.get(senderId));
    }

    @Transactional
    public MessageResponse sendAttachment(
            String conversationId,
            org.springframework.web.multipart.MultipartFile file,
            CurrentUser currentUser
    ) {
        String senderId = authorizationService.requireMemberContext(currentUser);
        authorizationService.requireConversationMember(conversationId, currentUser);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ChatServiceException(
                        HttpStatus.NOT_FOUND,
                        "CONVERSATION_NOT_FOUND",
                        "Conversation not found"
                ));

        ChatStorageService.StoredFile storedFile = storageService.storeAttachment(file);
        Message message = new Message();
        message.setConversation(conversation);
        message.setSenderId(senderId);
        message.setContent(storedFile.originalName());
        message.setAttachmentUrl(storedFile.url());
        message.setAttachmentName(storedFile.originalName());
        message.setAttachmentContentType(storedFile.contentType());
        message.setAttachmentSize(storedFile.size());
        message.setAttachmentKind(storedFile.kind());
        Message saved = messageRepository.save(message);
        Map<String, String> senderNames = memberDirectoryClient.findDisplayNames(List.of(senderId));
        return chatMapper.toResponse(saved, senderNames.get(senderId));
    }

    private String validateContent(ChatMessageRequest request) {
        if (request == null || request.content() == null || request.content().isBlank()) {
            throw new ChatServiceException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_MESSAGE_CONTENT",
                    "Message content is required"
            );
        }
        String content = request.content().trim();
        if (content.length() > 2000) {
            throw new ChatServiceException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_MESSAGE_CONTENT",
                    "Message content must not exceed 2000 characters"
            );
        }
        return content;
    }
}
