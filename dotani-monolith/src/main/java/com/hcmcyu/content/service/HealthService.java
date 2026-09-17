package com.hcmcyu.content.service;

import com.hcmcyu.content.dto.HealthResponse;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    public HealthResponse getHealth() {
        return new HealthResponse("content-service", "UP");
    }
}

