package com.dotani.shared.events;

import java.time.Instant;

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
}
