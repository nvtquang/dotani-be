package com.dotani.chat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dotani.chat.entity.Conversation;
import com.dotani.chat.entity.ConversationMember;
import com.dotani.chat.entity.ConversationType;
import com.dotani.chat.entity.Message;
import com.dotani.chat.repository.ConversationRepository;
import com.dotani.chat.repository.MessageRepository;
import com.dotani.member.entity.Member;
import com.dotani.support.IntegrationTest;
import com.dotani.support.TestData.Fixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ChatReadStateIntegrationTest extends IntegrationTest {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    private Fixture f;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        f = data.standardFixture();
        conversation = new Conversation();
        conversation.setType(ConversationType.DIRECT);
        conversation.setCreatedBy(f.tdp1Member().getId());
        for (Member member : new Member[] {f.tdp1Member(), f.tdp1Secretary()}) {
            ConversationMember conversationMember = new ConversationMember();
            conversationMember.setMemberId(member.getId());
            conversation.addMember(conversationMember);
        }
        conversation = conversationRepository.save(conversation);
    }

    @Test
    void unreadCountCountsOnlyOtherMembersMessagesUntilMarkedRead() throws Exception {
        sendMessage(f.tdp1Secretary(), "Chao em");
        sendMessage(f.tdp1Secretary(), "Nho tham gia hop nhe");
        sendMessage(f.tdp1Member(), "Vang a");

        mockMvc.perform(auth.as(f.tdp1Member(), get("/api/chat/conversations")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].unreadCount").value(2));
        mockMvc.perform(auth.as(f.tdp1Secretary(), get("/api/chat/conversations")))
                .andExpect(jsonPath("$.content[0].unreadCount").value(1));

        mockMvc.perform(auth.as(f.tdp1Member(), patch("/api/chat/conversations/{id}/read", conversation.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));

        mockMvc.perform(auth.as(f.tdp1Member(), get("/api/chat/conversations")))
                .andExpect(jsonPath("$.content[0].unreadCount").value(0));
        // marking read is per member: the other side still has its unread message
        mockMvc.perform(auth.as(f.tdp1Secretary(), get("/api/chat/conversations")))
                .andExpect(jsonPath("$.content[0].unreadCount").value(1));
    }

    @Test
    void outsiderCannotMarkConversationRead() throws Exception {
        mockMvc.perform(auth.as(f.tdp2Member(), patch("/api/chat/conversations/{id}/read", conversation.getId())))
                .andExpect(status().isForbidden());
    }

    private void sendMessage(Member sender, String content) {
        Message message = new Message();
        message.setConversation(conversation);
        message.setSenderId(sender.getId());
        message.setContent(content);
        messageRepository.save(message);
    }
}
