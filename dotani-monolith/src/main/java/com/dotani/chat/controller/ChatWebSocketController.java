package com.dotani.chat.controller;

import com.dotani.chat.dto.ChatMessageRequest;
import com.dotani.chat.dto.MessageResponse;
import com.dotani.chat.security.ChatPrincipal;
import com.dotani.chat.service.MessageService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.dotani.chat.dto.ChatTypingEvent;
import com.dotani.member.service.MemberDirectoryService;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Controller
@Validated
@Hidden
public class ChatWebSocketController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MemberDirectoryService memberDirectoryService;

    public ChatWebSocketController(
            MessageService messageService,
            SimpMessagingTemplate messagingTemplate,
            MemberDirectoryService memberDirectoryService
    ) {
        this.messageService = messageService;
        this.messagingTemplate = messagingTemplate;
        this.memberDirectoryService = memberDirectoryService;
    }

    @MessageMapping("/chat/{conversationId}")
    public void send(
            @DestinationVariable("conversationId") String conversationId,
            @Valid ChatMessageRequest request,
            Principal principal
    ) {
        ChatPrincipal chatPrincipal = (ChatPrincipal) principal;
        MessageResponse message = messageService.sendMessage(conversationId, request, chatPrincipal.currentUser());
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, message);
    }

    /** "Is typing" signal: relayed to the other members, never stored. */
    @MessageMapping("/chat/{conversationId}/typing")
    public void typing(@DestinationVariable("conversationId") String conversationId, Principal principal) {
        ChatPrincipal chatPrincipal = (ChatPrincipal) principal;
        String memberId = chatPrincipal.currentUser().memberId();
        messagingTemplate.convertAndSend(
                "/topic/conversations/" + conversationId + "/typing",
                new ChatTypingEvent(memberId, memberDirectoryService.findDisplayNames(java.util.List.of(memberId)).get(memberId))
        );
    }
}
