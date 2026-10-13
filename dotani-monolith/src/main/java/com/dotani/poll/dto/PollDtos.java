package com.dotani.poll.dto;

import com.dotani.poll.entity.PollStatus;
import com.dotani.poll.entity.ResultsVisibility;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/** Requests and responses of the poll (khảo sát / biểu quyết) API. */
public final class PollDtos {

    private PollDtos() {
    }

    /** @param open true to open the poll right away instead of keeping it as a draft (creation only) */
    public record PollRequest(
            @NotBlank
            @Size(max = 255)
            String title,

            @Size(max = 4000)
            String description,

            @NotBlank
            @Size(max = 36)
            String organizationId,

            @Size(max = 36)
            String eventId,

            @NotNull
            Boolean anonymous,

            @NotNull
            @Min(1)
            @Max(20)
            Integer maxChoices,

            @NotNull
            ResultsVisibility resultsVisibility,

            Instant closesAt,

            @NotNull
            @Size(min = 2, max = 20)
            List<@NotBlank @Size(max = 255) String> options,

            Boolean open
    ) {
    }

    public record VoteRequest(
            @NotEmpty
            @Size(max = 20)
            List<@NotBlank @Size(max = 36) String> optionIds
    ) {
    }

    /**
     * @param voteCount null while the results are hidden from the caller
     * @param voters    who chose the option; only for officers managing an open (non-secret) vote
     */
    public record OptionResponse(String id, String label, int position, Long voteCount, List<Voter> voters) {
    }

    public record Voter(String memberId, String fullName) {
    }

    public record PollResponse(
            String id,
            String title,
            String description,
            String organizationId,
            String organizationName,
            String eventId,
            boolean anonymous,
            int maxChoices,
            ResultsVisibility resultsVisibility,
            PollStatus status,
            Instant closesAt,
            Instant openedAt,
            Instant closedAt,
            String createdById,
            String createdByName,
            Instant createdAt,
            List<OptionResponse> options,
            long eligibleCount,
            long ballotCount,
            boolean eligible,
            boolean hasVoted,
            List<String> myOptionIds,
            boolean resultsVisible,
            boolean canManage
    ) {
    }
}
