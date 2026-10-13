package com.dotani.report.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Responses of the statistics API. Rates are 0..1, or null when nobody was invited. */
public final class ReportDtos {

    private ReportDtos() {
    }

    public enum PeriodGranularity {
        MONTH,
        QUARTER
    }

    /**
     * Membership statistics. Demographics (gender, age) count active members; status counts every member.
     *
     * @param ageGroups UNDER_16, 16_20, 21_25, 26_30, OVER_30, UNKNOWN (no date of birth)
     */
    public record MemberReport(
            long totalMembers,
            long activeMembers,
            Map<String, Long> statusCounts,
            Map<String, Long> genderCounts,
            List<Count> ageGroups,
            Double averageAge,
            List<OrganizationMembers> byOrganization
    ) {
    }

    public record Count(String key, long count) {
    }

    public record OrganizationMembers(
            String organizationId,
            String organizationName,
            long total,
            long active,
            long male,
            long female,
            long otherOrUnknown
    ) {
    }

    /**
     * Event participation of a year. For every published or completed event, the invited members are the
     * active members of the event's organization (within the report's scope); "going" are those of them who
     * registered GOING.
     */
    public record ParticipationReport(
            int year,
            PeriodGranularity granularity,
            List<ParticipationRow> periods,
            ParticipationRow total,
            List<OrganizationParticipation> byOrganization,
            List<EventRow> events
    ) {
    }

    public record ParticipationRow(
            String key,
            String label,
            long eventCount,
            long invitedCount,
            long goingCount,
            long notGoingCount,
            long undecidedCount,
            Double rate
    ) {
    }

    public record OrganizationParticipation(String organizationId, String organizationName, long invitedCount, long goingCount, Double rate) {
    }

    public record EventRow(
            String eventId,
            String title,
            Instant startTime,
            String organizationId,
            String organizationName,
            long invitedCount,
            long goingCount,
            Double rate
    ) {
    }
}
