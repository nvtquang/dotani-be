package com.hcmcyu.notification.config;

import com.hcmcyu.notification.entity.Notification;
import com.hcmcyu.notification.entity.NotificationType;
import com.hcmcyu.notification.entity.ReferenceType;
import com.hcmcyu.notification.entity.UserNotification;
import com.hcmcyu.notification.repository.NotificationRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

    private final NotificationRepository notificationRepository;

    public DevDataSeeder(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void run(String... args) {
        if (notificationRepository.count() > 0) {
            return;
        }

        List<String> allMembers = demoMemberIds();
        seedNotification(
                NotificationType.SYSTEM,
                "Chào mừng đến với HCMCYU",
                "Đây là thông báo mẫu cho môi trường development.",
                ReferenceType.SYSTEM,
                null,
                allMembers
        );
        seedNotification(
                NotificationType.EVENT_NEW,
                "Có sự kiện mới",
                "Mời đoàn viên kiểm tra lịch hoạt động sắp tới.",
                ReferenceType.EVENT,
                "dev-event",
                allMembers
        );
        seedNotification(
                NotificationType.POST_NEW,
                "Có bài viết mới",
                "Đoàn phường vừa cập nhật một bài viết mới.",
                ReferenceType.POST,
                "dev-post",
                allMembers
        );
        for (int tdp = 1; tdp <= 5; tdp++) {
            seedNotification(
                    NotificationType.MEETING_SCHEDULE,
                    "Lịch họp TDP " + tdp,
                    "Chi đoàn TDP " + tdp + " có lịch sinh hoạt mẫu.",
                    ReferenceType.EVENT,
                    "dev-tdp-" + tdp + "-meeting",
                    tdpMemberIds(tdp)
            );
        }
    }

    private void seedNotification(
            NotificationType notificationType,
            String title,
            String content,
            ReferenceType referenceType,
            String referenceId,
            List<String> recipientMemberIds
    ) {
        Notification notification = new Notification();
        notification.setNotificationType(notificationType);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);
        recipientMemberIds.forEach(memberId -> {
            UserNotification userNotification = new UserNotification();
            userNotification.setMemberId(memberId);
            notification.addRecipient(userNotification);
        });
        notificationRepository.save(notification);
    }

    private List<String> demoMemberIds() {
        List<String> memberIds = new ArrayList<>();
        memberIds.add("ward-secretary-member");
        memberIds.add("ward-deputy-member");
        for (int tdp = 1; tdp <= 5; tdp++) {
            memberIds.addAll(tdpMemberIds(tdp));
        }
        return memberIds;
    }

    private List<String> tdpMemberIds(int tdp) {
        List<String> memberIds = new ArrayList<>();
        memberIds.add("tdp-" + tdp + "-secretary-member");
        memberIds.add("tdp-" + tdp + "-deputy-member");
        for (int member = 1; member <= 5; member++) {
            memberIds.add("tdp-" + tdp + "-member-" + member);
        }
        return memberIds;
    }
}
