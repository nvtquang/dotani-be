package com.hcmcyu.chat.service;

import com.hcmcyu.chat.exception.ChatServiceException;
import com.hcmcyu.chat.repository.ConversationMemberRepository;
import com.hcmcyu.chat.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ChatAuthorizationService {

    private final ConversationMemberRepository conversationMemberRepository;

    public ChatAuthorizationService(ConversationMemberRepository conversationMemberRepository) {
        this.conversationMemberRepository = conversationMemberRepository;
    }

    public String requireMemberContext(CurrentUser currentUser) {
        if (currentUser == null || currentUser.memberId() == null || currentUser.memberId().isBlank()) {
            throw new ChatServiceException(
                    HttpStatus.FORBIDDEN,
                    "MEMBER_CONTEXT_REQUIRED",
                    "Current user is not linked to a member profile"
            );
        }
        return currentUser.memberId();
    }

    public void requireConversationMember(String conversationId, CurrentUser currentUser) {
        String memberId = requireMemberContext(currentUser);
        if (!conversationMemberRepository.existsByConversation_IdAndMemberId(conversationId, memberId)) {
            throw new ChatServiceException(
                    HttpStatus.FORBIDDEN,
                    "CONVERSATION_ACCESS_DENIED",
                    "Current member cannot access this conversation"
            );
        }
    }
}
