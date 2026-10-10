package com.dotani.notification.dto;

import com.dotani.notification.entity.NotificationType;
import com.dotani.notification.entity.ReferenceType;
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
