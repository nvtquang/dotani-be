package com.hcmcyu.auth.service;

import com.hcmcyu.auth.dto.HealthResponse;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    public HealthResponse getHealth() {
        return new HealthResponse("auth-service", "UP");
    }
}

