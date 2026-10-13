package com.dotani.shared.events;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Facts published by the event and content modules after they change data. The notification module
 * reacts to them after the transaction commits, so a failure there never rolls back the change itself.
 */
public final class DomainEvents {

    private DomainEvents() {
    }

    /** An event became visible to members: created as PUBLISHED, or published from a draft. */
    public record EventPublished(String eventId, String title, String description, String organizationId) {
    }

    /** A published event's schedule or details changed. */
    public record EventChanged(String eventId, String title, String organizationId) {
    }

    /** A published event was cancelled (status CANCELLED) or deleted. */
    public record EventCancelled(String eventId, String title, String organizationId, boolean deleted) {
    }

    /** Registration for a published event closes soon. */
    public record EventDeadlineApproaching(String eventId, String title, String organizationId, Instant deadline) {
    }

    /** A post became visible to members: created as PUBLISHED, or published from a draft. */
    public record PostPublished(String postId, String title, String organizationId, String authorMemberId) {
    }

    /** Officers asked members who have not paid a fee period yet to pay. */
    public record FeeReminderRequested(String periodId, String periodName, long amount, LocalDate dueDate, List<String> memberIds) {
    }

    /** A member asked (or was proposed) to move to another TDP; the deciding officers should look at it. */
    public record MemberTransferRequested(
            String transferId,
            String memberId,
            String memberName,
            String toOrganizationId,
            String toOrganizationName,
            String requestedByMemberId
    ) {
    }

    /** A transfer request was approved or rejected. */
    public record MemberTransferDecided(String transferId, String memberId, boolean approved, String toOrganizationName) {
    }

    /** Someone commented on a post. */
    public record PostCommented(String postId, String postTitle, String postAuthorMemberId, String commenterMemberId, String commenterName) {
    }

    /** A poll opened for voting. */
    public record PollOpened(String pollId, String title, String organizationId) {
    }
}
