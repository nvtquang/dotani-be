package com.dotani.poll.repository;

import com.dotani.poll.entity.PollBallot;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PollBallotRepository extends JpaRepository<PollBallot, PollBallot.Key> {

    @Query("select ballot.id.pollId, count(ballot) from PollBallot ballot where ballot.id.pollId in :pollIds group by ballot.id.pollId")
    List<Object[]> countGroupedByPoll(@Param("pollIds") Collection<String> pollIds);

    @Query("select ballot.id.pollId from PollBallot ballot where ballot.id.pollId in :pollIds and ballot.id.memberId = :memberId")
    List<String> findPollIdsVotedBy(@Param("pollIds") Collection<String> pollIds, @Param("memberId") String memberId);

    default Map<String, Long> counts(Collection<String> pollIds) {
        Map<String, Long> counts = new HashMap<>();
        if (!pollIds.isEmpty()) {
            countGroupedByPoll(pollIds).forEach(row -> counts.put((String) row[0], ((Number) row[1]).longValue()));
        }
        return counts;
    }
}
