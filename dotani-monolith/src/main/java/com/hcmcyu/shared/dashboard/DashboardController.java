package com.hcmcyu.shared.dashboard;

import com.hcmcyu.auth.security.AuthPrincipal;
import com.hcmcyu.content.service.ContentDashboardService;
import com.hcmcyu.event.service.EventDashboardService;
import com.hcmcyu.member.service.MemberDashboardService;
import com.hcmcyu.notification.service.NotificationManagementService;
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
    public DashboardSummaryResponse summary(@AuthenticationPrincipal AuthPrincipal principal) {
        return new DashboardSummaryResponse(
                memberDashboardService.getSummary(memberUser(principal)),
                eventDashboardService.getSummary(eventUser(principal)),
                contentDashboardService.getSummary(contentUser(principal)),
                notificationManagementService.countUnread(notificationUser(principal))
        );
    }

    private com.hcmcyu.member.security.CurrentUser memberUser(AuthPrincipal principal) {
        return new com.hcmcyu.member.security.CurrentUser(
                principal.userId(),
                principal.memberId(),
                principal.username(),
                com.hcmcyu.member.security.Role.valueOf(principal.role().name()),
                principal.organizationId(),
                principal.tdpId()
        );
    }

    private com.hcmcyu.event.security.CurrentUser eventUser(AuthPrincipal principal) {
        return new com.hcmcyu.event.security.CurrentUser(
                principal.userId(),
                principal.memberId(),
                principal.username(),
                com.hcmcyu.event.security.Role.valueOf(principal.role().name()),
                principal.organizationId(),
                principal.tdpId()
        );
    }

    private com.hcmcyu.content.security.CurrentUser contentUser(AuthPrincipal principal) {
        return new com.hcmcyu.content.security.CurrentUser(
                principal.userId(),
                principal.memberId(),
                principal.username(),
                com.hcmcyu.content.security.Role.valueOf(principal.role().name()),
                principal.organizationId(),
                principal.tdpId()
        );
    }

    private com.hcmcyu.notification.security.CurrentUser notificationUser(AuthPrincipal principal) {
        return new com.hcmcyu.notification.security.CurrentUser(
                principal.userId(),
                principal.memberId(),
                principal.username(),
                principal.email(),
                com.hcmcyu.notification.security.Role.valueOf(principal.role().name()),
                principal.organizationId(),
                principal.tdpId()
        );
    }
}
