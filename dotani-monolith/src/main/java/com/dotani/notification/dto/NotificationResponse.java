package com.dotani.notification.dto;

import com.dotani.notification.entity.NotificationType;
import com.dotani.notification.entity.ReferenceType;
import java.time.Instant;

public record NotificationResponse(
        String id,
        String userNotificationId,
        NotificationType notificationType,
        String title,
        String content,
        ReferenceType referenceType,
        String referenceId,
        boolean read,
        Instant readAt,
        Instant createdAt
) {
}
