package com.hcmcyu.notification.controller;

import com.hcmcyu.notification.dto.NotificationResponse;
import com.hcmcyu.notification.dto.UnreadCountResponse;
import com.hcmcyu.notification.security.CurrentUser;
import com.hcmcyu.notification.service.NotificationManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Current member notification APIs.")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT Bearer token"),
        @ApiResponse(responseCode = "403", description = "Notification owner denied"),
        @ApiResponse(responseCode = "404", description = "Notification not found")
})
public class NotificationController {

    private final NotificationManagementService notificationManagementService;

    public NotificationController(NotificationManagementService notificationManagementService) {
        this.notificationManagementService = notificationManagementService;
    }

    @GetMapping
    @Operation(summary = "List my notifications", description = "Paginated notifications for the current member only.")
    public Page<NotificationResponse> findMine(
            @AuthenticationPrincipal CurrentUser currentUser,
            Pageable pageable
    ) {
        return notificationManagementService.findMine(currentUser, pageable);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Count unread notifications", description = "Returns unread notification count for the current member.")
    public UnreadCountResponse countUnread(@AuthenticationPrincipal CurrentUser currentUser) {
        return notificationManagementService.countUnread(currentUser);
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark notification as read", description = "Marks a notification as read only when it belongs to the current member.")
    public NotificationResponse markRead(
            @PathVariable("id") String id,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return notificationManagementService.markRead(id, currentUser);
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all notifications as read", description = "Marks all current member notifications as read and returns remaining unread count.")
    public UnreadCountResponse markAllRead(@AuthenticationPrincipal CurrentUser currentUser) {
        return notificationManagementService.markAllRead(currentUser);
    }
}
