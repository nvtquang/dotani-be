package com.dotani.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/** A member's reaction to a post; one per member and post, changing it replaces the type. */
@Entity
@Table(name = "post_reactions")
public class PostReaction {

    @Embeddable
    public static class Key implements Serializable {

        @Column(name = "post_id", length = 36)
        private String postId;

        @Column(name = "member_id", length = 36)
        private String memberId;

        protected Key() {
        }

        public Key(String postId, String memberId) {
            this.postId = postId;
            this.memberId = memberId;
        }

        public String getPostId() {
            return postId;
        }

        public String getMemberId() {
            return memberId;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key key && Objects.equals(postId, key.postId) && Objects.equals(memberId, key.memberId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(postId, memberId);
        }
    }

    @EmbeddedId
    private Key id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReactionType reaction;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PostReaction() {
    }

    public PostReaction(String postId, String memberId, ReactionType reaction) {
        this.id = new Key(postId, memberId);
        this.reaction = reaction;
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

    public Key getId() {
        return id;
    }

    public ReactionType getReaction() {
        return reaction;
    }

    public void setReaction(ReactionType reaction) {
        this.reaction = reaction;
    }
}
