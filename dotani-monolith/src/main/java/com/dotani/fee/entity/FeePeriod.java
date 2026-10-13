package com.dotani.fee.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** A đoàn phí collection: every active member of the organization (ward or TDP) owes {@code amount}. */
@Entity
@Table(name = "fee_periods")
public class FeePeriod {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "organization_id", nullable = false, length = 36)
    private String organizationId;

    @Column(nullable = false)
    private long amount;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "collector_member_id", length = 36)
    private String collectorMemberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FeePeriodStatus status = FeePeriodStatus.OPEN;

    @Column(name = "last_reminded_at")
    private Instant lastRemindedAt;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getCollectorMemberId() {
        return collectorMemberId;
    }

    public void setCollectorMemberId(String collectorMemberId) {
        this.collectorMemberId = collectorMemberId;
    }

    public FeePeriodStatus getStatus() {
        return status;
    }

    public void setStatus(FeePeriodStatus status) {
        this.status = status;
    }

    public Instant getLastRemindedAt() {
        return lastRemindedAt;
    }

    public void markReminded(Instant now) {
        lastRemindedAt = now;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
