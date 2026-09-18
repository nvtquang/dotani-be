package com.hcmcyu.shared.integration;

import com.hcmcyu.event.entity.Event;
import com.hcmcyu.event.service.EventNotificationPublisher;
import com.hcmcyu.member.entity.MemberStatus;
import com.hcmcyu.member.repository.MemberRepository;
import com.hcmcyu.notification.dto.InternalNotificationCreateRequest;
import com.hcmcyu.notification.entity.NotificationType;
import com.hcmcyu.notification.entity.ReferenceType;
import com.hcmcyu.notification.service.NotificationManagementService;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EventNotificationPublisherAdapter implements EventNotificationPublisher {

    private final MemberRepository memberRepository;
    private final NotificationManagementService notificationManagementService;
    private final String internalSecret;

    public EventNotificationPublisherAdapter(
            MemberRepository memberRepository,
            NotificationManagementService notificationManagementService,
            @Value("${notification.internal.secret}") String internalSecret
    ) {
        this.memberRepository = memberRepository;
        this.notificationManagementService = notificationManagementService;
        this.internalSecret = internalSecret;
    }

    @Override
    public void publishEventCreated(Event event) {
        List<String> recipientIds = memberRepository.findIdsByOrganizationIdAndStatus(
                event.getOrganizationId(),
                MemberStatus.ACTIVE
        );
        if (recipientIds.isEmpty()) {
            return;
        }

        notificationManagementService.createInternal(
                new InternalNotificationCreateRequest(
                        NotificationType.EVENT_NEW,
                        "Sự kiện mới: " + event.getTitle(),
                        event.getDescription() == null || event.getDescription().isBlank()
                                ? "Một sự kiện mới vừa được tạo trong tổ dân phố của bạn."
                                : event.getDescription(),
                        ReferenceType.EVENT,
                        event.getId(),
                        recipientIds
                ),
                internalSecret
        );
    }
}
