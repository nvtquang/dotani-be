package com.dotani.member.entity;

import com.dotani.shared.crypto.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

/** Bank details of a member, kept apart from the profile. The account number is encrypted at rest. */
@Entity
@Table(name = "member_banking")
public class MemberBanking {

    @Id
    @Column(name = "member_id", length = 36)
    private String memberId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "bank_code", length = 50)
    private String bankCode;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "account_number_encrypted", length = 512)
    private String accountNumber;

    @Column(name = "account_holder_name")
    private String accountHolderName;

    @Column(name = "bank_qr_image_url", length = 1000)
    private String bankQrImageUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MemberBanking() {
    }

    MemberBanking(Member member) {
        this.member = member;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public String getBankQrImageUrl() {
        return bankQrImageUrl;
    }

    public void setBankQrImageUrl(String bankQrImageUrl) {
        this.bankQrImageUrl = bankQrImageUrl;
    }
}
