package com.dotani.content.mapper;

import com.dotani.member.service.NameLookup;
import java.util.List;
import org.springframework.data.domain.Page;
import com.dotani.content.dto.PostImageResponse;
import com.dotani.content.dto.PostRequest;
import com.dotani.content.dto.PostResponse;
import com.dotani.content.entity.Post;
import org.springframework.stereotype.Component;

@Component
public class PostMapper {

    private final NameLookup nameLookup;

    public PostMapper(NameLookup nameLookup) {
        this.nameLookup = nameLookup;
    }


    public PostResponse toResponse(Post post) {
        return toResponse(post, names(List.of(post)));
    }

    /** Maps a page with one name lookup for all of its items. */
    public Page<PostResponse> toResponsePage(Page<Post> page) {
        NameLookup.Names names = names(page.getContent());
        return page.map(post -> toResponse(post, names));
    }

    private NameLookup.Names names(List<Post> items) {
        return nameLookup.of(
                items.stream().map(post -> post.getAuthorId()).toList(),
                items.stream().map(post -> post.getOrganizationId()).toList()
        );
    }

    private PostResponse toResponse(Post post, NameLookup.Names names) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getType(),
                post.getOrganizationId(),
                names.organization(post.getOrganizationId()),
                post.getAuthorId(),
                names.member(post.getAuthorId()),
                post.getStatus(),
                post.getAttachments().stream()
                        .map(image -> new PostImageResponse(
                                image.getId(),
                                image.getUrl(),
                                image.getKind().name(),
                                image.getName(),
                                image.getCreatedAt()
                        ))
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
