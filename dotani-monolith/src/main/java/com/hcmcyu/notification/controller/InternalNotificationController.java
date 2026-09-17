package com.hcmcyu.notification.controller;

import com.hcmcyu.notification.dto.InternalNotificationCreateRequest;
import com.hcmcyu.notification.dto.InternalNotificationCreateResponse;
import com.hcmcyu.notification.service.NotificationManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/notifications")
@Tag(name = "Internal Notifications", description = "Internal service-to-service notification creation APIs. Protected by X-Internal-Secret.")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid internal secret")
})
public class InternalNotificationController {

    private final NotificationManagementService notificationManagementService;

    public InternalNotificationController(NotificationManagementService notificationManagementService) {
        this.notificationManagementService = notificationManagementService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create notifications internally", description = "Called by other services to create user notifications without sharing databases.")
    public InternalNotificationCreateResponse create(
            @RequestHeader(name = "X-Internal-Secret", required = false) String internalSecret,
            @Valid @RequestBody InternalNotificationCreateRequest request
    ) {
        return notificationManagementService.createInternal(request, internalSecret);
    }
}
