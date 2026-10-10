package com.dotani.shared.integration;

import com.dotani.event.entity.Event;
import com.dotani.event.service.EventNotificationPublisher;
import com.dotani.member.entity.MemberStatus;
import com.dotani.member.repository.MemberRepository;
import com.dotani.notification.dto.NotificationCreateRequest;
import com.dotani.notification.entity.NotificationType;
import com.dotani.notification.entity.ReferenceType;
import com.dotani.notification.service.NotificationManagementService;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class EventNotificationPublisherAdapter implements EventNotificationPublisher {

    private final MemberRepository memberRepository;
    private final NotificationManagementService notificationManagementService;

    public EventNotificationPublisherAdapter(
            MemberRepository memberRepository,
            NotificationManagementService notificationManagementService
    ) {
        this.memberRepository = memberRepository;
        this.notificationManagementService = notificationManagementService;
    }

    @Override
    public void publishEventCreated(Event event) {
        // Ward-level events must reach members of every TDP under the ward, not only members attached to the ward itself.
        List<String> recipientIds = memberRepository.findIdsInOrganizationTreeAndStatus(
                event.getOrganizationId(),
                MemberStatus.ACTIVE
        );
        if (recipientIds.isEmpty()) {
            return;
        }

        notificationManagementService.create(
                new NotificationCreateRequest(
                        NotificationType.EVENT_NEW,
                        "Sự kiện mới: " + event.getTitle(),
                        event.getDescription() == null || event.getDescription().isBlank()
                                ? "Một sự kiện mới vừa được tạo trong tổ dân phố của bạn."
                                : event.getDescription(),
                        ReferenceType.EVENT,
                        event.getId(),
                        recipientIds
                )
        );
    }
}
