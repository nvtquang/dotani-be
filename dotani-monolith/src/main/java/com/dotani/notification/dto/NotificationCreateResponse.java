package com.dotani.notification.dto;

public record NotificationCreateResponse(
        String notificationId,
        int recipientCount
) {
}
