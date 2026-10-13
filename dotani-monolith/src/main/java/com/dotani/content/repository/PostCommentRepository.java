package com.dotani.content.repository;

import com.dotani.content.entity.PostComment;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostCommentRepository extends JpaRepository<PostComment, String> {

    Page<PostComment> findByPostId(String postId, Pageable pageable);

    Optional<PostComment> findByIdAndPostId(String id, String postId);

    @Query("select comment.postId, count(comment) from PostComment comment "
            + "where comment.postId in :postIds group by comment.postId")
    List<Object[]> countGroupedByPost(@Param("postIds") Collection<String> postIds);

    /** Comment count per post id; posts without comments are absent. */
    default Map<String, Long> counts(Collection<String> postIds) {
        Map<String, Long> counts = new HashMap<>();
        if (!postIds.isEmpty()) {
            countGroupedByPost(postIds).forEach(row -> counts.put((String) row[0], ((Number) row[1]).longValue()));
        }
        return counts;
    }
}
