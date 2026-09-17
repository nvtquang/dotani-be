package com.hcmcyu.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "member_role_change_audits")
public class MemberRoleChangeAudit {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "member_id", nullable = false, length = 36)
    private String memberId;

    @Column(name = "user_id", length = 36)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_role", nullable = false, length = 50)
    private MemberRole oldRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_role", nullable = false, length = 50)
    private MemberRole newRole;

    @Column(name = "actor_user_id", nullable = false, length = 36)
    private String actorUserId;

    @Column(name = "actor_member_id", length = 36)
    private String actorMemberId;

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

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public MemberRole getOldRole() {
        return oldRole;
    }

    public void setOldRole(MemberRole oldRole) {
        this.oldRole = oldRole;
    }

    public MemberRole getNewRole() {
        return newRole;
    }

    public void setNewRole(MemberRole newRole) {
        this.newRole = newRole;
    }

    public String getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(String actorUserId) {
        this.actorUserId = actorUserId;
    }

    public String getActorMemberId() {
        return actorMemberId;
    }

    public void setActorMemberId(String actorMemberId) {
        this.actorMemberId = actorMemberId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
