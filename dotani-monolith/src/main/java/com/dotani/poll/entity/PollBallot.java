package com.dotani.poll.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/** That a member voted in a poll (not what they chose): one per member, also for secret ballots. */
@Entity
@Table(name = "poll_ballots")
public class PollBallot {

    @Embeddable
    public static class Key implements Serializable {

        @Column(name = "poll_id", length = 36)
        private String pollId;

        @Column(name = "member_id", length = 36)
        private String memberId;

        protected Key() {
        }

        public Key(String pollId, String memberId) {
            this.pollId = pollId;
            this.memberId = memberId;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key key && Objects.equals(pollId, key.pollId) && Objects.equals(memberId, key.memberId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(pollId, memberId);
        }
    }

    @EmbeddedId
    private Key id;

    @Column(name = "voted_at", nullable = false)
    private Instant votedAt;

    protected PollBallot() {
    }

    public PollBallot(String pollId, String memberId, Instant votedAt) {
        this.id = new Key(pollId, memberId);
        this.votedAt = votedAt;
    }
}
