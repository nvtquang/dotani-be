package com.dotani.notification.service;

import com.dotani.notification.dto.NotificationCreateRequest;
import com.dotani.notification.dto.NotificationCreateResponse;
import com.dotani.notification.dto.NotificationResponse;
import com.dotani.notification.dto.UnreadCountResponse;
import com.dotani.notification.entity.Notification;
import com.dotani.notification.entity.UserNotification;
import com.dotani.shared.exception.ApiException;
import com.dotani.notification.mapper.NotificationMapper;
import com.dotani.notification.repository.NotificationRepository;
import com.dotani.notification.repository.UserNotificationRepository;
import com.dotani.shared.security.CurrentUser;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationManagementService {

    private final NotificationRepository notificationRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final NotificationMapper notificationMapper;
    private final NotificationSecurityService notificationSecurityService;

    public NotificationManagementService(
            NotificationRepository notificationRepository,
            UserNotificationRepository userNotificationRepository,
            NotificationMapper notificationMapper,
            NotificationSecurityService notificationSecurityService
    ) {
        this.notificationRepository = notificationRepository;
        this.userNotificationRepository = userNotificationRepository;
        this.notificationMapper = notificationMapper;
        this.notificationSecurityService = notificationSecurityService;
    }

    @Transactional
    public NotificationCreateResponse create(NotificationCreateRequest request) {
        Set<String> recipientIds = new LinkedHashSet<>(request.recipientMemberIds());
        Notification notification = new Notification();
        notification.setNotificationType(request.notificationType());
        notification.setTitle(request.title());
        notification.setContent(request.content());
        notification.setReferenceType(request.referenceType());
        notification.setReferenceId(request.referenceId());

        recipientIds.forEach(memberId -> {
            UserNotification userNotification = new UserNotification();
            userNotification.setMemberId(memberId);
            notification.addRecipient(userNotification);
        });

        Notification saved = notificationRepository.save(notification);
        return new NotificationCreateResponse(saved.getId(), recipientIds.size());
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> findMine(CurrentUser currentUser, Pageable pageable) {
        String memberId = notificationSecurityService.requireMemberId(currentUser);
        return userNotificationRepository.findByMemberId(memberId, pageable)
                .map(notificationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse countUnread(CurrentUser currentUser) {
        String memberId = notificationSecurityService.requireMemberId(currentUser);
        return new UnreadCountResponse(userNotificationRepository.countByMemberIdAndReadAtIsNull(memberId));
    }

    @Transactional
    public NotificationResponse markRead(String id, CurrentUser currentUser) {
        String memberId = notificationSecurityService.requireMemberId(currentUser);
        UserNotification userNotification = userNotificationRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "NOTIFICATION_NOT_FOUND",
                        "Notification was not found"
                ));
        if (!userNotification.getMemberId().equals(memberId)) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "OUT_OF_SCOPE",
                    "You cannot access this notification"
            );
        }
        userNotification.markRead();
        return notificationMapper.toResponse(userNotification);
    }

    @Transactional
    public UnreadCountResponse markAllRead(CurrentUser currentUser) {
        String memberId = notificationSecurityService.requireMemberId(currentUser);
        userNotificationRepository.markAllReadByMemberId(memberId);
        return new UnreadCountResponse(0);
    }
}
