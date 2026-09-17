package com.hcmcyu.content.repository;

import com.hcmcyu.content.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PostRepository extends JpaRepository<Post, String>, JpaSpecificationExecutor<Post> {
}
