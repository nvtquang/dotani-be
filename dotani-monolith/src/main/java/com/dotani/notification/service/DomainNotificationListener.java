package com.dotani.notification.service;

import com.dotani.event.repository.EventParticipationRepository;
import com.dotani.member.entity.MemberStatus;
import com.dotani.member.repository.MemberRepository;
import com.dotani.notification.dto.NotificationCreateRequest;
import com.dotani.notification.entity.NotificationType;
import com.dotani.notification.entity.ReferenceType;
import com.dotani.shared.events.DomainEvents.EventCancelled;
import com.dotani.shared.events.DomainEvents.EventChanged;
import com.dotani.shared.events.DomainEvents.EventDeadlineApproaching;
import com.dotani.shared.events.DomainEvents.EventPublished;
import com.dotani.shared.events.DomainEvents.PostPublished;
import com.dotani.shared.time.AppTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Turns domain events into notifications. Runs after the change has committed, on the async executor,
 * in its own transaction: notifying never slows down or rolls back the user's action.
 *
 * Recipients: everyone active in the organization (a ward reaches all of its TDPs) for new events and
 * posts; members who answered the event for changes and cancellations; members who have not answered yet
 * for deadline reminders.
 */
@Component
public class DomainNotificationListener {

    private static final DateTimeFormatter DEADLINE_FORMAT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").withZone(AppTime.ZONE);

    private final NotificationManagementService notificationManagementService;
    private final MemberRepository memberRepository;
    private final EventParticipationRepository participationRepository;

    public DomainNotificationListener(
            NotificationManagementService notificationManagementService,
            MemberRepository memberRepository,
            EventParticipationRepository participationRepository
    ) {
        this.notificationManagementService = notificationManagementService;
        this.memberRepository = memberRepository;
        this.participationRepository = participationRepository;
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(EventPublished event) {
        notify(
                activeMembersOf(event.organizationId()),
                NotificationType.EVENT_NEW,
                "Sự kiện mới: " + event.title(),
                event.description() == null || event.description().isBlank()
                        ? "Một sự kiện mới vừa được đăng trong đơn vị của bạn."
                        : event.description(),
                ReferenceType.EVENT,
                event.eventId()
        );
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(EventChanged event) {
        notify(
                participantsOf(event.eventId()),
                NotificationType.EVENT_UPDATED,
                "Sự kiện đã thay đổi: " + event.title(),
                "Thông tin sự kiện bạn đã phản hồi vừa được cập nhật. Vui lòng xem lại thời gian và địa điểm.",
                ReferenceType.EVENT,
                event.eventId()
        );
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(EventCancelled event) {
        notify(
                participantsOf(event.eventId()),
                NotificationType.EVENT_CANCELLED,
                "Sự kiện đã huỷ: " + event.title(),
                "Sự kiện bạn đã phản hồi không còn diễn ra.",
                // a deleted event can no longer be opened, so the notification does not link to it
                event.deleted() ? null : ReferenceType.EVENT,
                event.deleted() ? null : event.eventId()
        );
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(EventDeadlineApproaching event) {
        Set<String> answered = Set.copyOf(participantsOf(event.eventId()));
        List<String> notAnswered = activeMembersOf(event.organizationId()).stream()
                .filter(memberId -> !answered.contains(memberId))
                .toList();
        notify(
                notAnswered,
                NotificationType.EVENT_DEADLINE,
                "Sắp hết hạn đăng ký: " + event.title(),
                "Hạn đăng ký tham gia là " + DEADLINE_FORMAT.format(event.deadline()) + ". Hãy phản hồi nếu bạn chưa phản hồi.",
                ReferenceType.EVENT,
                event.eventId()
        );
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(PostPublished post) {
        List<String> recipients = activeMembersOf(post.organizationId()).stream()
                .filter(memberId -> !memberId.equals(post.authorMemberId()))
                .toList();
        notify(
                recipients,
                NotificationType.POST_NEW,
                "Bài viết mới: " + post.title(),
                "Có bài viết mới trong đơn vị của bạn.",
                ReferenceType.POST,
                post.postId()
        );
    }

    private List<String> activeMembersOf(String organizationId) {
        return memberRepository.findIdsInOrganizationTreeAndStatus(organizationId, MemberStatus.ACTIVE);
    }

    private List<String> participantsOf(String eventId) {
        return participationRepository.findByEvent_IdOrderByUpdatedAtDesc(eventId).stream()
                .map(participation -> participation.getMemberId())
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new))
                .stream()
                .toList();
    }

    private void notify(
            List<String> recipients,
            NotificationType type,
            String title,
            String content,
            ReferenceType referenceType,
            String referenceId
    ) {
        if (recipients.isEmpty()) {
            return;
        }
        String safeTitle = title.length() > 255 ? title.substring(0, 252) + "..." : title;
        notificationManagementService.create(new NotificationCreateRequest(
                type, safeTitle, content, referenceType, referenceId, recipients));
    }
}
