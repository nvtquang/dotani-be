package com.dotani.poll.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** One chosen option. memberId is null in a secret ballot, so a vote cannot be traced back. */
@Entity
@Table(name = "poll_answers")
public class PollAnswer {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "poll_id", nullable = false, length = 36)
    private String pollId;

    @Column(name = "option_id", nullable = false, length = 36)
    private String optionId;

    @Column(name = "member_id", length = 36)
    private String memberId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PollAnswer() {
    }

    public PollAnswer(String pollId, String optionId, String memberId) {
        this.pollId = pollId;
        this.optionId = optionId;
        this.memberId = memberId;
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        createdAt = Instant.now();
    }

    public String getOptionId() {
        return optionId;
    }

    public String getMemberId() {
        return memberId;
    }
}
