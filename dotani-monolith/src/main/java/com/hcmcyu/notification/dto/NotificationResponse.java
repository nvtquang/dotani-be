package com.hcmcyu.notification.dto;

import com.hcmcyu.notification.entity.NotificationType;
import com.hcmcyu.notification.entity.ReferenceType;
import java.time.LocalDateTime;

public record NotificationResponse(
        String id,
        String userNotificationId,
        NotificationType notificationType,
        String title,
        String content,
        ReferenceType referenceType,
        String referenceId,
        boolean read,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {
}
