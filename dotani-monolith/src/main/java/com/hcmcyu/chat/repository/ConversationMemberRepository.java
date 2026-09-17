package com.hcmcyu.chat.repository;

import com.hcmcyu.chat.entity.ConversationMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, String> {

    boolean existsByConversation_IdAndMemberId(String conversationId, String memberId);

    Optional<ConversationMember> findByConversation_IdAndMemberId(String conversationId, String memberId);

    List<ConversationMember> findByMemberIdOrderByCreatedAtDesc(String memberId);
}
