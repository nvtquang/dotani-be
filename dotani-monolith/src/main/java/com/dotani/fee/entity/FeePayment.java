package com.dotani.fee.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

/** One member's payment for one fee period, recorded by an officer. */
@Entity
@Table(
        name = "fee_payments",
        uniqueConstraints = @UniqueConstraint(name = "uk_fee_payments_period_member", columnNames = {"period_id", "member_id"})
)
public class FeePayment {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "period_id", nullable = false, length = 36)
    private String periodId;

    @Column(name = "member_id", nullable = false, length = 36)
    private String memberId;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Column(name = "paid_at", nullable = false)
    private Instant paidAt;

    @Column(length = 500)
    private String note;

    @Column(name = "recorded_by", length = 36)
    private String recordedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getPeriodId() {
        return periodId;
    }

    public void setPeriodId(String periodId) {
        this.periodId = periodId;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(String recordedBy) {
        this.recordedBy = recordedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
