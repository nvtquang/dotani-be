package com.hcmcyu.notification.service;

import com.hcmcyu.notification.exception.NotificationServiceException;
import com.hcmcyu.notification.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class NotificationSecurityService {

    public String requireMemberId(CurrentUser currentUser) {
        if (currentUser == null || currentUser.memberId() == null || currentUser.memberId().isBlank()) {
            throw new NotificationServiceException(
                    HttpStatus.FORBIDDEN,
                    "MEMBER_CONTEXT_REQUIRED",
                    "Member context is required"
            );
        }
        return currentUser.memberId();
    }
}
