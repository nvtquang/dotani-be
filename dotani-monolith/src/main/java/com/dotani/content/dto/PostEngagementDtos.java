package com.dotani.content.dto;

import com.dotani.content.entity.ReactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Requests and responses for comments and reactions on posts. */
public final class PostEngagementDtos {

    private PostEngagementDtos() {
    }

    public record CommentRequest(
            @NotBlank
            @Size(max = 2000)
            String content
    ) {
    }

    public record CommentResponse(
            String id,
            String postId,
            String authorId,
            String authorName,
            String authorAvatarUrl,
            String content,
            Instant createdAt,
            Instant updatedAt,
            boolean edited,
            boolean canEdit,
            boolean canDelete
    ) {
    }

    public record ReactionRequest(@NotNull ReactionType type) {
    }

    public record EngagementLookupRequest(
            @NotEmpty
            @Size(max = 100)
            List<@NotBlank @Size(max = 36) String> postIds
    ) {
    }

    /** Reactions per type, the caller's own reaction (null if none) and the number of comments. */
    public record EngagementResponse(
            String postId,
            Map<ReactionType, Long> reactionCounts,
            long totalReactions,
            ReactionType myReaction,
            long commentCount
    ) {
    }
}
