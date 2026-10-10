package com.dotani.notification.service;

import com.dotani.shared.exception.ApiException;
import com.dotani.shared.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class NotificationSecurityService {

    public String requireMemberId(CurrentUser currentUser) {
        if (currentUser == null || currentUser.memberId() == null || currentUser.memberId().isBlank()) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "MEMBER_CONTEXT_REQUIRED",
                    "Member context is required"
            );
        }
        return currentUser.memberId();
    }
}
