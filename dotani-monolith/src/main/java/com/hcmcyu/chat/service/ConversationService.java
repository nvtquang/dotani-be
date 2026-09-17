package com.hcmcyu.chat.service;

import com.hcmcyu.chat.dto.ConversationCreateRequest;
import com.hcmcyu.chat.dto.ConversationMemberRequest;
import com.hcmcyu.chat.dto.ConversationResponse;
import com.hcmcyu.chat.entity.Conversation;
import com.hcmcyu.chat.entity.ConversationMember;
import com.hcmcyu.chat.entity.ConversationType;
import com.hcmcyu.chat.exception.ChatServiceException;
import com.hcmcyu.chat.mapper.ChatMapper;
import com.hcmcyu.chat.repository.ConversationMemberRepository;
import com.hcmcyu.chat.repository.ConversationRepository;
import com.hcmcyu.chat.security.CurrentUser;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final ChatAuthorizationService authorizationService;
    private final ChatMapper chatMapper;
    private final MemberDirectoryClient memberDirectoryClient;
    private final ChatStorageService storageService;

    public ConversationService(
            ConversationRepository conversationRepository,
            ConversationMemberRepository conversationMemberRepository,
            ChatAuthorizationService authorizationService,
            ChatMapper chatMapper,
            MemberDirectoryClient memberDirectoryClient,
            ChatStorageService storageService
    ) {
        this.conversationRepository = conversationRepository;
        this.conversationMemberRepository = conversationMemberRepository;
        this.authorizationService = authorizationService;
        this.chatMapper = chatMapper;
        this.memberDirectoryClient = memberDirectoryClient;
        this.storageService = storageService;
    }

    @Transactional
    public ConversationResponse create(ConversationCreateRequest request, CurrentUser currentUser) {
        String currentMemberId = authorizationService.requireMemberContext(currentUser);
        Set<String> memberIds = new LinkedHashSet<>(request.memberIds());
        memberIds.add(currentMemberId);
        validateConversationMembers(request.type(), memberIds);

        Conversation conversation = new Conversation();
        conversation.setType(request.type());
        conversation.setTitle(normalizeTitle(request.title()));
        conversation.setCreatedBy(currentMemberId);
        memberIds.forEach(memberId -> addMemberEntity(conversation, memberId));

        return toResponse(conversationRepository.saveAndFlush(conversation));
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> findMine(CurrentUser currentUser) {
        String currentMemberId = authorizationService.requireMemberContext(currentUser);
        return conversationMemberRepository.findByMemberIdOrderByCreatedAtDesc(currentMemberId)
                .stream()
                .map(ConversationMember::getConversation)
                .map(conversation -> conversationRepository.findByIdWithMembers(conversation.getId()).orElseThrow())
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConversationResponse findById(String id, CurrentUser currentUser) {
        authorizationService.requireConversationMember(id, currentUser);
        return toResponse(getConversationWithMembers(id));
    }

    @Transactional
    public ConversationResponse addMember(
            String conversationId,
            ConversationMemberRequest request,
            CurrentUser currentUser
    ) {
        authorizationService.requireConversationMember(conversationId, currentUser);
        Conversation conversation = getConversationWithMembers(conversationId);
        if (conversation.getType() != ConversationType.GROUP) {
            throw new ChatServiceException(
                    HttpStatus.BAD_REQUEST,
                    "DIRECT_CONVERSATION_IMMUTABLE",
                    "Cannot add members to a direct conversation"
            );
        }
        if (conversationMemberRepository.existsByConversation_IdAndMemberId(conversationId, request.memberId())) {
            return toResponse(conversation);
        }

        addMemberEntity(conversation, request.memberId());
        try {
            return toResponse(conversationRepository.saveAndFlush(conversation));
        } catch (DataIntegrityViolationException exception) {
            return toResponse(getConversationWithMembers(conversationId));
        }
    }

    @Transactional
    public void removeMember(String conversationId, String memberId, CurrentUser currentUser) {
        authorizationService.requireConversationMember(conversationId, currentUser);
        Conversation conversation = getConversationWithMembers(conversationId);
        if (conversation.getType() != ConversationType.GROUP) {
            throw new ChatServiceException(
                    HttpStatus.BAD_REQUEST,
                    "DIRECT_CONVERSATION_IMMUTABLE",
                    "Cannot remove members from a direct conversation"
            );
        }
        ConversationMember member = conversationMemberRepository.findByConversation_IdAndMemberId(conversationId, memberId)
                .orElseThrow(() -> new ChatServiceException(
                        HttpStatus.NOT_FOUND,
                        "CONVERSATION_MEMBER_NOT_FOUND",
                        "Conversation member not found"
                ));
        conversation.removeMember(member);
    }

    @Transactional
    public ConversationResponse updateGroupAvatar(
            String conversationId,
            org.springframework.web.multipart.MultipartFile file,
            CurrentUser currentUser
    ) {
        authorizationService.requireConversationMember(conversationId, currentUser);
        Conversation conversation = getConversationWithMembers(conversationId);
        if (conversation.getType() != ConversationType.GROUP) {
            throw new ChatServiceException(
                    HttpStatus.BAD_REQUEST,
                    "DIRECT_CONVERSATION_IMMUTABLE",
                    "Cannot update avatar for a direct conversation"
            );
        }
        conversation.setAvatarUrl(storageService.storeGroupAvatar(file));
        return toResponse(conversationRepository.saveAndFlush(conversation));
    }

    private Conversation getConversationWithMembers(String id) {
        return conversationRepository.findByIdWithMembers(id)
                .orElseThrow(() -> new ChatServiceException(
                        HttpStatus.NOT_FOUND,
                        "CONVERSATION_NOT_FOUND",
                        "Conversation not found"
                ));
    }

    private ConversationResponse toResponse(Conversation conversation) {
        Map<String, String> memberNames = memberDirectoryClient.findDisplayNames(conversation.getMembers().stream()
                .map(ConversationMember::getMemberId)
                .toList());
        return chatMapper.toResponse(conversation, memberNames);
    }

    private void validateConversationMembers(ConversationType type, Set<String> memberIds) {
        if (type == ConversationType.DIRECT && memberIds.size() != 2) {
            throw new ChatServiceException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_DIRECT_CONVERSATION",
                    "Direct conversation must have exactly two members"
            );
        }
        if (type == ConversationType.GROUP && memberIds.size() < 2) {
            throw new ChatServiceException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_GROUP_CONVERSATION",
                    "Group conversation must have at least two members"
            );
        }
    }

    private void addMemberEntity(Conversation conversation, String memberId) {
        if (memberId == null || memberId.isBlank()) {
            throw new ChatServiceException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_CONVERSATION_MEMBER",
                    "Conversation member is required"
            );
        }
        ConversationMember member = new ConversationMember();
        member.setMemberId(memberId.trim());
        conversation.addMember(member);
    }

    private String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return null;
        }
        return title.trim();
    }
}
