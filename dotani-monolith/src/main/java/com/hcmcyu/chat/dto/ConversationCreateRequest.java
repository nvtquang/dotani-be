package com.hcmcyu.chat.dto;

import com.hcmcyu.chat.entity.ConversationType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ConversationCreateRequest(
        @NotNull
        ConversationType type,

        @Size(max = 255)
        String title,

        @NotEmpty
        List<@Size(max = 36) String> memberIds
) {
}
