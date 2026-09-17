package com.hcmcyu.notification.dto;

import com.hcmcyu.notification.entity.NotificationType;
import com.hcmcyu.notification.entity.ReferenceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record InternalNotificationCreateRequest(
        @NotNull
        NotificationType notificationType,

        @NotBlank
        @Size(max = 255)
        String title,

        @NotBlank
        String content,

        ReferenceType referenceType,

        @Size(max = 36)
        String referenceId,

        @NotEmpty
        List<@NotBlank @Size(max = 36) String> recipientMemberIds
) {
}
