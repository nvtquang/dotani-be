package com.hcmcyu.auth.dto;

public record GoogleUserInfo(
        String subject,
        String email,
        String name
) {
}
