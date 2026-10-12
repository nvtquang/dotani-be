package com.dotani.member.entity;

import org.hibernate.annotations.SQLRestriction;
import com.dotani.auth.entity.UserAccount;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// Soft-deleted rows (deleted_at set) are invisible to every JPA query on this entity.
@SQLRestriction("deleted_at IS NULL")
@Entity
@Table(name = "members")
public class Member {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", unique = true, length = 36)
    private String userId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    @Column(length = 30)
    private String phone;

    // Contact email for members without a login account. Linked members use the account's email
    // ; this column then stays null so an email is never stored twice.
    @Column(length = 255)
    private String email;

    // Read-only view of the login account referenced by user_id, used to search by login email.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private UserAccount account;

    @Column(length = 500)
    private String address;

    @Column(name = "avatar_url", length = 1000)
    private String avatarUrl;

    // Bank details live in member_banking; the accessors below read and write through it.
    @OneToOne(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private MemberBanking banking;

    @Column(name = "youth_union_join_date")
    private LocalDate youthUnionJoinDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_status", nullable = false, length = 30)
    private MemberStatus memberStatus = MemberStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_role", nullable = false, length = 50)
    private MemberRole memberRole = MemberRole.MEMBER;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationUnit organization;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

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

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }


    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getBankName() {
        return banking == null ? null : banking.getBankName();
    }

    public void setBankName(String bankName) {
        if (bankName == null && banking == null) {
            return;
        }
        banking().setBankName(bankName);
    }

    public String getBankCode() {
        return banking == null ? null : banking.getBankCode();
    }

    public void setBankCode(String bankCode) {
        if (bankCode == null && banking == null) {
            return;
        }
        banking().setBankCode(bankCode);
    }

    public String getAccountNumber() {
        return banking == null ? null : banking.getAccountNumber();
    }

    public void setAccountNumber(String accountNumber) {
        if (accountNumber == null && banking == null) {
            return;
        }
        banking().setAccountNumber(accountNumber);
    }

    public String getAccountHolderName() {
        return banking == null ? null : banking.getAccountHolderName();
    }

    public void setAccountHolderName(String accountHolderName) {
        if (accountHolderName == null && banking == null) {
            return;
        }
        banking().setAccountHolderName(accountHolderName);
    }

    public String getBankQrImageUrl() {
        return banking == null ? null : banking.getBankQrImageUrl();
    }

    public void setBankQrImageUrl(String bankQrImageUrl) {
        if (bankQrImageUrl == null && banking == null) {
            return;
        }
        banking().setBankQrImageUrl(bankQrImageUrl);
    }

    public LocalDate getYouthUnionJoinDate() {
        return youthUnionJoinDate;
    }

    public void setYouthUnionJoinDate(LocalDate youthUnionJoinDate) {
        this.youthUnionJoinDate = youthUnionJoinDate;
    }

    public MemberStatus getMemberStatus() {
        return memberStatus;
    }

    public void setMemberStatus(MemberStatus memberStatus) {
        this.memberStatus = memberStatus;
    }

    public MemberRole getMemberRole() {
        return memberRole;
    }

    public void setMemberRole(MemberRole memberRole) {
        this.memberRole = memberRole;
    }

    public OrganizationUnit getOrganization() {
        return organization;
    }

    public void setOrganization(OrganizationUnit organization) {
        this.organization = organization;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private MemberBanking banking() {
        if (banking == null) {
            banking = new MemberBanking(this);
        }
        return banking;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void markDeleted() {
        if (deletedAt == null) {
            deletedAt = Instant.now();
        }
    }
}
