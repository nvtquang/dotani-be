package com.hcmcyu.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConversationMemberRequest(
        @NotBlank
        @Size(max = 36)
        String memberId
) {
}
