package com.dotani.auth.service;

import com.dotani.auth.entity.UserAccount;
import com.dotani.auth.repository.UserAccountRepository;
import com.dotani.shared.exception.ApiException;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Locks an account for a while after too many failed logins. Counters are written in their own
 * transaction so a failed login (which throws) still records the attempt.
 */
@Service
public class LoginAttemptService {

    private final UserAccountRepository userAccountRepository;
    private final int maxAttempts;
    private final Duration lockDuration;

    public LoginAttemptService(
            UserAccountRepository userAccountRepository,
            @Value("${app.security.login.max-attempts:5}") int maxAttempts,
            @Value("${app.security.login.lock-minutes:15}") long lockMinutes
    ) {
        this.userAccountRepository = userAccountRepository;
        this.maxAttempts = maxAttempts;
        this.lockDuration = Duration.ofMinutes(lockMinutes);
    }

    public void requireNotLocked(UserAccount user) {
        Instant lockedUntil = user.getLockedUntil();
        if (lockedUntil != null && lockedUntil.isAfter(Instant.now())) {
            long minutes = Math.max(1, Duration.between(Instant.now(), lockedUntil).toMinutes() + 1);
            throw new ApiException(
                    HttpStatus.LOCKED,
                    "ACCOUNT_LOCKED",
                    "Account is locked for " + minutes + " more minute(s) after too many failed logins",
                    minutes
            );
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String userId) {
        userAccountRepository.findById(userId).ifPresent(user -> {
            int attempts = user.getFailedLoginAttempts() + 1;
            if (attempts >= maxAttempts) {
                user.setLockedUntil(Instant.now().plus(lockDuration));
                user.setFailedLoginAttempts(0);
            } else {
                user.setFailedLoginAttempts(attempts);
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(String userId) {
        userAccountRepository.findById(userId).ifPresent(user -> {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
        });
    }
}
