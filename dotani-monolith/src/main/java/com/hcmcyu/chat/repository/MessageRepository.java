package com.hcmcyu.chat.repository;

import com.hcmcyu.chat.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, String> {

    Page<Message> findByConversation_Id(String conversationId, Pageable pageable);
}
