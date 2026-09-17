package com.hcmcyu.chat.repository;

import com.hcmcyu.chat.entity.Conversation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, String> {

    @Query("""
            select distinct conversation
            from Conversation conversation
            left join fetch conversation.members
            where conversation.id = :id
            """)
    Optional<Conversation> findByIdWithMembers(@Param("id") String id);
}
