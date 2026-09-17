package com.hcmcyu.member.service;

import com.hcmcyu.member.dto.HealthResponse;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    public HealthResponse getHealth() {
        return new HealthResponse("member-service", "UP");
    }
}

