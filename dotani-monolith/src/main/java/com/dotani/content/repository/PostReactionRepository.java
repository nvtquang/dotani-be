package com.dotani.content.repository;

import com.dotani.content.entity.PostReaction;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostReactionRepository extends JpaRepository<PostReaction, PostReaction.Key> {

    @Query("select reaction.id.postId, reaction.reaction, count(reaction) from PostReaction reaction "
            + "where reaction.id.postId in :postIds group by reaction.id.postId, reaction.reaction")
    List<Object[]> countGroupedByPostAndType(@Param("postIds") Collection<String> postIds);

    @Query("select reaction from PostReaction reaction "
            + "where reaction.id.postId in :postIds and reaction.id.memberId = :memberId")
    List<PostReaction> findMine(@Param("postIds") Collection<String> postIds, @Param("memberId") String memberId);
}
