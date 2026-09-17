package com.hcmcyu.notification.dto;

public record InternalNotificationCreateResponse(
        String notificationId,
        int recipientCount
) {
}
