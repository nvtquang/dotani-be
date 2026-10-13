package com.dotani.emulation.dto;

import com.dotani.emulation.entity.AwardForm;
import com.dotani.emulation.entity.AwardKind;
import com.dotani.emulation.entity.RatingLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Requests and responses of the emulation (thi đua) API. */
public final class EmulationDtos {

    private EmulationDtos() {
    }

    public record AwardRequest(
            @NotBlank
            @Size(max = 36)
            String memberId,

            @NotNull
            AwardForm form,

            @NotBlank
            @Size(max = 255)
            String title,

            @Size(max = 4000)
            String reason,

            @Size(max = 100)
            String decisionNumber,

            @NotNull
            @PastOrPresent
            LocalDate decisionDate,

            @Size(max = 255)
            String issuer
    ) {
    }

    public record AwardResponse(
            String id,
            String memberId,
            AwardForm form,
            AwardKind kind,
            String title,
            String reason,
            String decisionNumber,
            LocalDate decisionDate,
            String issuer,
            String createdById,
            String createdByName,
            Instant createdAt
    ) {
    }

    public record RatingRequest(
            @NotBlank
            @Size(max = 36)
            String memberId,

            @NotNull
            Integer year,

            @NotNull
            RatingLevel rating,

            @Size(max = 1000)
            String note
    ) {
    }

    public record RatingResponse(
            String id,
            String memberId,
            int year,
            RatingLevel rating,
            String note,
            String ratedById,
            String ratedByName,
            Instant updatedAt
    ) {
    }

    /** Hồ sơ thi đua of one member: yearly ratings and decisions, newest first. */
    public record ProfileResponse(
            String memberId,
            String memberName,
            String organizationName,
            boolean canManage,
            List<RatingResponse> ratings,
            List<AwardResponse> awards
    ) {
    }

    /** One line of the yearly rating board: a member and the rating they got that year (null if not rated). */
    public record BoardRow(
            String memberId,
            String fullName,
            String organizationId,
            String organizationName,
            String avatarUrl,
            String ratingId,
            RatingLevel rating,
            String note,
            boolean canManage
    ) {
    }

    /** @param ratingCounts members per rating, plus UNRATED */
    public record SummaryResponse(
            int year,
            long memberCount,
            Map<String, Long> ratingCounts,
            long commendationCount,
            long disciplineCount
    ) {
    }
}
