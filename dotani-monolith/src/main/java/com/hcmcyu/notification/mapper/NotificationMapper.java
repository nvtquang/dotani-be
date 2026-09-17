package com.hcmcyu.notification.mapper;

import com.hcmcyu.notification.dto.NotificationResponse;
import com.hcmcyu.notification.entity.Notification;
import com.hcmcyu.notification.entity.UserNotification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(UserNotification userNotification) {
        Notification notification = userNotification.getNotification();
        return new NotificationResponse(
                notification.getId(),
                userNotification.getId(),
                notification.getNotificationType(),
                notification.getTitle(),
                notification.getContent(),
                notification.getReferenceType(),
                notification.getReferenceId(),
                userNotification.getReadAt() != null,
                userNotification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}
