package com.hcmcyu.event.service;

import com.hcmcyu.event.dto.HealthResponse;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    public HealthResponse getHealth() {
        return new HealthResponse("event-service", "UP");
    }
}

