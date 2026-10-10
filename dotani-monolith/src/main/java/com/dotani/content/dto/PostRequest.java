package com.dotani.content.dto;

import com.dotani.content.entity.PostStatus;
import com.dotani.content.entity.PostType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostRequest(
        @NotBlank
        @Size(max = 255)
        String title,

        @NotBlank
        String content,

        @NotNull
        PostType type,

        @NotBlank
        @Size(max = 36)
        String organizationId,

        @NotNull
        PostStatus status
) {
}
