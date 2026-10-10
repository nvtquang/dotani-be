package com.dotani.content.repository;

import com.dotani.content.entity.PostImage;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostImageRepository extends JpaRepository<PostImage, String> {

    Optional<PostImage> findByIdAndPost_Id(String id, String postId);
}
