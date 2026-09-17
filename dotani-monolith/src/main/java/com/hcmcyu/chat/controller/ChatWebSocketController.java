package com.hcmcyu.chat.controller;

import com.hcmcyu.chat.dto.ChatMessageRequest;
import com.hcmcyu.chat.dto.MessageResponse;
import com.hcmcyu.chat.security.ChatPrincipal;
import com.hcmcyu.chat.service.MessageService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Controller
@Validated
@Hidden
public class ChatWebSocketController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(MessageService messageService, SimpMessagingTemplate messagingTemplate) {
        this.messageService = messageService;
        this.messagingTemplate = messagingTemplate;
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
}
