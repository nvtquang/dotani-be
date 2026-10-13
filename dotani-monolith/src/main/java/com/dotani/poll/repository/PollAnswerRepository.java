package com.dotani.poll.repository;

import com.dotani.poll.entity.PollAnswer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PollAnswerRepository extends JpaRepository<PollAnswer, String> {

    @Query("select answer.optionId, count(answer) from PollAnswer answer where answer.pollId = :pollId group by answer.optionId")
    List<Object[]> countGroupedByOption(@Param("pollId") String pollId);

    List<PollAnswer> findByPollId(String pollId);

    List<PollAnswer> findByPollIdAndMemberId(String pollId, String memberId);
}
