package com.hcmcyu.audit.service;

import com.hcmcyu.audit.dto.HealthResponse;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    public HealthResponse getHealth() {
        return new HealthResponse("audit-service", "UP");
    }
}
