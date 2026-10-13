package com.dotani.poll.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.SQLRestriction;

/** A survey or vote among the active members of an organization, optionally tied to an event (đại hội). */
// Soft-deleted rows (deleted_at set) are invisible to every JPA query on this entity.
@SQLRestriction("deleted_at IS NULL")
@Entity
@Table(name = "polls")
public class Poll {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "organization_id", nullable = false, length = 36)
    private String organizationId;

    @Column(name = "event_id", length = 36)
    private String eventId;

    /** secret ballot: answers are stored without the member */
    @Column(nullable = false)
    private boolean anonymous;

    @Column(name = "max_choices", nullable = false)
    private int maxChoices = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "results_visibility", nullable = false, length = 20)
    private ResultsVisibility resultsVisibility = ResultsVisibility.AFTER_VOTE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PollStatus status = PollStatus.DRAFT;

    @Column(name = "closes_at")
    private Instant closesAt;

    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<PollOption> options = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    /** OPEN with a closing time already past counts as closed, even before the closing job runs. */
    public PollStatus effectiveStatus(Instant now) {
        if (status == PollStatus.OPEN && closesAt != null && !closesAt.isAfter(now)) {
            return PollStatus.CLOSED;
        }
        return status;
    }

    public void open(Instant now) {
        status = PollStatus.OPEN;
        openedAt = now;
    }

    public void close(Instant now) {
        status = PollStatus.CLOSED;
        closedAt = now;
    }

    public void replaceOptions(List<String> labels) {
        options.clear();
        for (int i = 0; i < labels.size(); i++) {
            options.add(new PollOption(this, labels.get(i), i));
        }
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public void setAnonymous(boolean anonymous) {
        this.anonymous = anonymous;
    }

    public int getMaxChoices() {
        return maxChoices;
    }

    public void setMaxChoices(int maxChoices) {
        this.maxChoices = maxChoices;
    }

    public ResultsVisibility getResultsVisibility() {
        return resultsVisibility;
    }

    public void setResultsVisibility(ResultsVisibility resultsVisibility) {
        this.resultsVisibility = resultsVisibility;
    }

    public PollStatus getStatus() {
        return status;
    }

    public Instant getClosesAt() {
        return closesAt;
    }

    public void setClosesAt(Instant closesAt) {
        this.closesAt = closesAt;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<PollOption> getOptions() {
        return options;
    }

    public void markDeleted() {
        if (deletedAt == null) {
            deletedAt = Instant.now();
        }
    }
}
