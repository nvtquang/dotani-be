package com.hcmcyu.chat.repository;

import com.hcmcyu.chat.entity.Message;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, String> {

    Page<Message> findByConversation_Id(String conversationId, Pageable pageable);

    Optional<Message> findTopByConversation_IdOrderByCreatedAtDesc(String conversationId);

    long countByConversation_IdAndSenderIdNot(String conversationId, String senderId);

    long countByConversation_IdAndCreatedAtAfterAndSenderIdNot(
            String conversationId,
            LocalDateTime createdAt,
            String senderId
    );
}
