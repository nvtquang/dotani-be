package com.hcmcyu.notification.controller;

import com.hcmcyu.notification.dto.HealthResponse;
import com.hcmcyu.notification.service.HealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Health", description = "Public service health endpoint.")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping("/health")
    @Operation(summary = "Notification service health check", description = "Public endpoint used to verify that notification-service is running.")
    public HealthResponse health() {
        return healthService.getHealth();
    }
}
