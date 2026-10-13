package com.dotani.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** A move of a member from one TDP to another: a request until decided, then a line of the member's history. */
@Entity
@Table(name = "member_transfers")
public class MemberTransfer {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "member_id", nullable = false, length = 36)
    private String memberId;

    @Column(name = "from_organization_id", nullable = false, length = 36)
    private String fromOrganizationId;

    @Column(name = "to_organization_id", nullable = false, length = 36)
    private String toOrganizationId;

    @Column(length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransferStatus status = TransferStatus.PENDING;

    @Column(name = "requested_by", length = 36)
    private String requestedBy;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "decided_by", length = 36)
    private String decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decision_note", length = 1000)
    private String decisionNote;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (requestedAt == null) {
            requestedAt = Instant.now();
        }
    }

    /** A change of TDP made directly on the member profile: recorded as an approved transfer. */
    public static MemberTransfer direct(String memberId, String fromOrganizationId, String toOrganizationId, String actorMemberId) {
        MemberTransfer transfer = new MemberTransfer();
        transfer.memberId = memberId;
        transfer.fromOrganizationId = fromOrganizationId;
        transfer.toOrganizationId = toOrganizationId;
        transfer.requestedBy = actorMemberId;
        transfer.decide(TransferStatus.APPROVED, actorMemberId, "Cập nhật trực tiếp trên hồ sơ đoàn viên");
        return transfer;
    }

    public void decide(TransferStatus decision, String deciderMemberId, String note) {
        status = decision;
        decidedBy = deciderMemberId;
        decidedAt = Instant.now();
        decisionNote = note;
    }

    public String getId() {
        return id;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getFromOrganizationId() {
        return fromOrganizationId;
    }

    public void setFromOrganizationId(String fromOrganizationId) {
        this.fromOrganizationId = fromOrganizationId;
    }

    public String getToOrganizationId() {
        return toOrganizationId;
    }

    public void setToOrganizationId(String toOrganizationId) {
        this.toOrganizationId = toOrganizationId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public String getDecidedBy() {
        return decidedBy;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public String getDecisionNote() {
        return decisionNote;
    }
}
