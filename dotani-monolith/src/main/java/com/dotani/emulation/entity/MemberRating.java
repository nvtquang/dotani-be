package com.dotani.emulation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

/** The rating a member received for one year. */
@Entity
@Table(
        name = "member_ratings",
        uniqueConstraints = @UniqueConstraint(name = "uk_member_ratings_member_year", columnNames = {"member_id", "rating_year"})
)
public class MemberRating {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "member_id", nullable = false, length = 36)
    private String memberId;

    @Column(name = "rating_year", nullable = false)
    private int year;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RatingLevel rating;

    @Column(length = 1000)
    private String note;

    @Column(name = "rated_by", length = 36)
    private String ratedBy;

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

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public RatingLevel getRating() {
        return rating;
    }

    public void setRating(RatingLevel rating) {
        this.rating = rating;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getRatedBy() {
        return ratedBy;
    }

    public void setRatedBy(String ratedBy) {
        this.ratedBy = ratedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
