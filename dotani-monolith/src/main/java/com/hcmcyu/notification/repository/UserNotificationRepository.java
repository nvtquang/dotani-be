package com.hcmcyu.notification.repository;

import com.hcmcyu.notification.entity.UserNotification;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserNotificationRepository extends JpaRepository<UserNotification, String> {

    @EntityGraph(attributePaths = "notification")
    Page<UserNotification> findByMemberId(String memberId, Pageable pageable);

    @EntityGraph(attributePaths = "notification")
    Optional<UserNotification> findByIdAndMemberId(String id, String memberId);

    long countByMemberIdAndReadAtIsNull(String memberId);

    @Modifying
    @Query("update UserNotification notification set notification.readAt = CURRENT_TIMESTAMP "
            + "where notification.memberId = :memberId and notification.readAt is null")
    int markAllReadByMemberId(@Param("memberId") String memberId);
}
