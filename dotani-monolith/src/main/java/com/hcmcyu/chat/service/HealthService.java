package com.hcmcyu.chat.service;

import com.hcmcyu.chat.dto.HealthResponse;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    public HealthResponse getHealth() {
        return new HealthResponse("chat-service", "UP");
    }
}

