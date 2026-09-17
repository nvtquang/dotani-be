package com.hcmcyu.notification.service;

import com.hcmcyu.notification.dto.InternalNotificationCreateRequest;
import com.hcmcyu.notification.dto.InternalNotificationCreateResponse;
import com.hcmcyu.notification.dto.NotificationResponse;
import com.hcmcyu.notification.dto.UnreadCountResponse;
import com.hcmcyu.notification.entity.Notification;
import com.hcmcyu.notification.entity.UserNotification;
import com.hcmcyu.notification.exception.NotificationServiceException;
import com.hcmcyu.notification.mapper.NotificationMapper;
import com.hcmcyu.notification.repository.NotificationRepository;
import com.hcmcyu.notification.repository.UserNotificationRepository;
import com.hcmcyu.notification.security.CurrentUser;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
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
    private final String internalSecret;

    public NotificationManagementService(
            NotificationRepository notificationRepository,
            UserNotificationRepository userNotificationRepository,
            NotificationMapper notificationMapper,
            NotificationSecurityService notificationSecurityService,
            @Value("${notification.internal.secret}") String internalSecret
    ) {
        this.notificationRepository = notificationRepository;
        this.userNotificationRepository = userNotificationRepository;
        this.notificationMapper = notificationMapper;
        this.notificationSecurityService = notificationSecurityService;
        this.internalSecret = internalSecret;
    }

    @Transactional
    public InternalNotificationCreateResponse createInternal(
            InternalNotificationCreateRequest request,
            String providedSecret
    ) {
        if (providedSecret == null || !providedSecret.equals(internalSecret)) {
            throw new NotificationServiceException(
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_INTERNAL_SECRET",
                    "Internal notification secret is invalid"
            );
        }

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
        return new InternalNotificationCreateResponse(saved.getId(), recipientIds.size());
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
                .orElseThrow(() -> new NotificationServiceException(
                        HttpStatus.NOT_FOUND,
                        "NOTIFICATION_NOT_FOUND",
                        "Notification was not found"
                ));
        if (!userNotification.getMemberId().equals(memberId)) {
            throw new NotificationServiceException(
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
