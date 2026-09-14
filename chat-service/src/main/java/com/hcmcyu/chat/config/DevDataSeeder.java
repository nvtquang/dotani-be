package com.hcmcyu.chat.config;

import com.hcmcyu.chat.entity.Conversation;
import com.hcmcyu.chat.entity.ConversationMember;
import com.hcmcyu.chat.entity.ConversationType;
import com.hcmcyu.chat.entity.Message;
import com.hcmcyu.chat.repository.ConversationRepository;
import com.hcmcyu.chat.repository.MessageRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public DevDataSeeder(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    @Override
    public void run(String... args) {
        if (conversationRepository.count() > 0) {
            return;
        }

        Conversation wardGroup = seedConversation(
                ConversationType.GROUP,
                "Nhóm Đoàn phường Thượng Cát",
                "ward-secretary-member",
                "ward-secretary-member",
                "ward-deputy-member",
                "tdp-1-secretary-member",
                "tdp-2-secretary-member",
                "tdp-3-secretary-member",
                "tdp-4-secretary-member",
                "tdp-5-secretary-member"
        );
        seedMessage(wardGroup, "ward-secretary-member", "Chào mừng các đồng chí đến với nhóm chat development.");
        seedMessage(wardGroup, "tdp-1-secretary-member", "TDP 1 đã nhận thông tin.");

        Conversation tdp1Group = seedConversation(
                ConversationType.GROUP,
                "Chi đoàn TDP 1",
                "tdp-1-secretary-member",
                "tdp-1-secretary-member",
                "tdp-1-deputy-member",
                "tdp-1-member-1",
                "tdp-1-member-2",
                "tdp-1-member-3",
                "tdp-1-member-4",
                "tdp-1-member-5"
        );
        seedMessage(tdp1Group, "tdp-1-secretary-member", "Mời mọi người theo dõi lịch sinh hoạt TDP 1.");
        seedMessage(tdp1Group, "tdp-1-member-1", "Em đã nhận được thông báo.");

        Conversation direct = seedConversation(
                ConversationType.DIRECT,
                null,
                "tdp-1-secretary-member",
                "tdp-1-secretary-member",
                "tdp-1-member-1"
        );
        seedMessage(direct, "tdp-1-secretary-member", "Cần hỗ trợ thông tin hồ sơ thì báo lại nhé.");
        seedMessage(direct, "tdp-1-member-1", "Em cảm ơn anh chị.");
    }

    private Conversation seedConversation(
            ConversationType type,
            String title,
            String createdBy,
            String... memberIds
    ) {
        Conversation conversation = new Conversation();
        conversation.setType(type);
        conversation.setTitle(title);
        conversation.setCreatedBy(createdBy);
        for (String memberId : memberIds) {
            ConversationMember member = new ConversationMember();
            member.setMemberId(memberId);
            conversation.addMember(member);
        }
        return conversationRepository.save(conversation);
    }

    private void seedMessage(Conversation conversation, String senderId, String content) {
        Message message = new Message();
        message.setConversation(conversation);
        message.setSenderId(senderId);
        message.setContent(content);
        messageRepository.save(message);
    }
}
