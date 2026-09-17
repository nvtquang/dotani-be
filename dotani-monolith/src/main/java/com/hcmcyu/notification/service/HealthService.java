package com.hcmcyu.notification.service;

import com.hcmcyu.notification.dto.HealthResponse;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    public HealthResponse getHealth() {
        return new HealthResponse("notification-service", "UP");
    }
}

