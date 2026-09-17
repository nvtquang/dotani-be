package com.hcmcyu.content.mapper;

import com.hcmcyu.content.dto.PostImageResponse;
import com.hcmcyu.content.dto.PostRequest;
import com.hcmcyu.content.dto.PostResponse;
import com.hcmcyu.content.entity.Post;
import org.springframework.stereotype.Component;

@Component
public class PostMapper {

    public PostResponse toResponse(Post post) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getType(),
                post.getOrganizationId(),
                post.getAuthorId(),
                post.getStatus(),
                post.getImages().stream()
                        .map(image -> new PostImageResponse(image.getId(), image.getImageUrl(), image.getCreatedAt()))
                        .toList(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    public void apply(Post post, PostRequest request) {
        post.setTitle(request.title().trim());
        post.setContent(request.content().trim());
        post.setType(request.type());
        post.setOrganizationId(request.organizationId().trim());
        post.setStatus(request.status());
    }
}
