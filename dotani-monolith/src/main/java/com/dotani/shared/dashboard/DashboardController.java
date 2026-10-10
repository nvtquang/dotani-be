package com.dotani.shared.dashboard;

import com.dotani.content.service.ContentDashboardService;
import com.dotani.event.service.EventDashboardService;
import com.dotani.member.service.MemberDashboardService;
import com.dotani.notification.service.NotificationManagementService;
import com.dotani.shared.security.CurrentUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final MemberDashboardService memberDashboardService;
    private final EventDashboardService eventDashboardService;
    private final ContentDashboardService contentDashboardService;
    private final NotificationManagementService notificationManagementService;

    public DashboardController(
            MemberDashboardService memberDashboardService,
            EventDashboardService eventDashboardService,
            ContentDashboardService contentDashboardService,
            NotificationManagementService notificationManagementService
    ) {
        this.memberDashboardService = memberDashboardService;
        this.eventDashboardService = eventDashboardService;
        this.contentDashboardService = contentDashboardService;
        this.notificationManagementService = notificationManagementService;
    }

    @GetMapping("/summary")
    public DashboardSummaryResponse summary(@AuthenticationPrincipal CurrentUser currentUser) {
        return new DashboardSummaryResponse(
                memberDashboardService.getSummary(currentUser),
                eventDashboardService.getSummary(currentUser),
                contentDashboardService.getSummary(currentUser),
                notificationManagementService.countUnread(currentUser)
        );
    }
}
