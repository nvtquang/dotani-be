package com.dotani.auth.dto;

public record GoogleUserInfo(
        String subject,
        String email,
        String name
) {
}
