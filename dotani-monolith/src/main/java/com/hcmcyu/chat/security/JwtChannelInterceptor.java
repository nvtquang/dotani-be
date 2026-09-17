package com.hcmcyu.chat.security;

import com.hcmcyu.chat.exception.ChatServiceException;
import com.hcmcyu.chat.repository.ConversationMemberRepository;
import java.security.Principal;
import java.util.List;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final ConversationMemberRepository conversationMemberRepository;

    public JwtChannelInterceptor(
            JwtService jwtService,
            ConversationMemberRepository conversationMemberRepository
    ) {
        this.jwtService = jwtService;
        this.conversationMemberRepository = conversationMemberRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            CurrentUser currentUser = jwtService.parseAccessToken(extractToken(accessor));
            if (currentUser.memberId() == null || currentUser.memberId().isBlank()) {
                throw new ChatServiceException(
                        org.springframework.http.HttpStatus.FORBIDDEN,
                        "MEMBER_CONTEXT_REQUIRED",
                        "Current user is not linked to a member profile"
                );
            }
            accessor.setUser(new ChatPrincipal(currentUser));
            return message;
        }

        CurrentUser currentUser = currentUser(accessor);
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String conversationId = conversationIdFromTopic(accessor.getDestination());
            requireConversationMember(conversationId, currentUser);
        }
        if (StompCommand.SEND.equals(accessor.getCommand())) {
            String conversationId = conversationIdFromApplicationDestination(accessor.getDestination());
            requireConversationMember(conversationId, currentUser);
        }
        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        List<String> authorizationHeaders = accessor.getNativeHeader("Authorization");
        String authorization = authorizationHeaders == null || authorizationHeaders.isEmpty()
                ? null
                : authorizationHeaders.getFirst();
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ChatServiceException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "UNAUTHORIZED",
                    "Authentication is required"
            );
        }
        return authorization.substring(7);
    }

    private CurrentUser currentUser(SimpMessageHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        if (user instanceof ChatPrincipal chatPrincipal) {
            return chatPrincipal.currentUser();
        }
        throw new ChatServiceException(
                org.springframework.http.HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED",
                "Authentication is required"
        );
    }

    private void requireConversationMember(String conversationId, CurrentUser currentUser) {
        if (conversationId == null
                || !conversationMemberRepository.existsByConversation_IdAndMemberId(conversationId, currentUser.memberId())) {
            throw new ChatServiceException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "CONVERSATION_ACCESS_DENIED",
                    "Current member cannot access this conversation"
            );
        }
    }

    private String conversationIdFromTopic(String destination) {
        String prefix = "/topic/conversations/";
        if (destination == null || !destination.startsWith(prefix)) {
            return null;
        }
        return destination.substring(prefix.length());
    }

    private String conversationIdFromApplicationDestination(String destination) {
        String prefix = "/app/chat/";
        if (destination == null || !destination.startsWith(prefix)) {
            return null;
        }
        return destination.substring(prefix.length());
    }
}
