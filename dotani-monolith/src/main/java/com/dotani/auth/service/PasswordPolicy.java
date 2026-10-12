package com.dotani.auth.service;

import com.dotani.shared.exception.ApiException;
import org.springframework.http.HttpStatus;

/** Minimum password rules for new passwords (registration, change, reset). */
final class PasswordPolicy {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 100;

    private PasswordPolicy() {
    }

    static void requireStrong(String password) {
        boolean ok = password != null
                && password.length() >= MIN_LENGTH
                && password.length() <= MAX_LENGTH
                && password.chars().anyMatch(Character::isLetter)
                && password.chars().anyMatch(Character::isDigit);
        if (!ok) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "WEAK_PASSWORD",
                    "Password must be 8-100 characters and contain letters and digits"
            );
        }
    }
}
