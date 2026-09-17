package com.hcmcyu.content.controller;

import com.hcmcyu.content.dto.HealthResponse;
import com.hcmcyu.content.service.HealthService;
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
    @Operation(summary = "Content service health check", description = "Public endpoint used to verify that content-service is running.")
    public HealthResponse health() {
        return healthService.getHealth();
    }
}
