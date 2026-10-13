package com.dotani.content.service;

import com.dotani.content.dto.PostEngagementDtos.CommentResponse;
import com.dotani.content.dto.PostEngagementDtos.EngagementResponse;
import com.dotani.content.entity.Post;
import com.dotani.content.entity.PostComment;
import com.dotani.content.entity.PostReaction;
import com.dotani.content.entity.PostStatus;
import com.dotani.content.entity.ReactionType;
import com.dotani.content.repository.PostCommentRepository;
import com.dotani.content.repository.PostReactionRepository;
import com.dotani.content.repository.PostRepository;
import com.dotani.member.service.MemberRoster;
import com.dotani.shared.events.DomainEvents.PostCommented;
import com.dotani.shared.exception.ApiException;
import com.dotani.shared.security.CurrentUser;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Comments and reactions. Anyone who can read a published post can react and comment; authors edit and
 * delete their own comments, ward officers (who manage posts) can delete any comment.
 */
@Service
public class PostEngagementService {

    private final PostRepository postRepository;
    private final PostCommentRepository commentRepository;
    private final PostReactionRepository reactionRepository;
    private final PostScopeService postScopeService;
    private final MemberRoster memberRoster;
    private final ApplicationEventPublisher events;

    public PostEngagementService(
            PostRepository postRepository,
            PostCommentRepository commentRepository,
            PostReactionRepository reactionRepository,
            PostScopeService postScopeService,
            MemberRoster memberRoster,
            ApplicationEventPublisher events
    ) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.reactionRepository = reactionRepository;
        this.postScopeService = postScopeService;
        this.memberRoster = memberRoster;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> comments(String postId, Pageable pageable, CurrentUser currentUser) {
        getReadablePost(postId, currentUser);
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("createdAt", "id"));
        Page<PostComment> page = commentRepository.findByPostId(postId, sorted);
        Map<String, MemberRoster.Card> authors = memberRoster.cards(page.map(PostComment::getMemberId).getContent());
        return page.map(comment -> toResponse(comment, authors, currentUser));
    }

    @Transactional
    public CommentResponse addComment(String postId, String content, CurrentUser currentUser) {
        Post post = getPublishedPost(postId, currentUser);
        PostComment comment = new PostComment();
        comment.setPostId(postId);
        comment.setMemberId(requireMemberId(currentUser));
        comment.setContent(content.trim());
        PostComment saved = commentRepository.save(comment);
        Map<String, MemberRoster.Card> authors = memberRoster.cards(List.of(saved.getMemberId()));
        MemberRoster.Card author = authors.get(saved.getMemberId());
        events.publishEvent(new PostCommented(post.getId(), post.getTitle(), post.getAuthorId(), saved.getMemberId(),
                author == null ? "Một đoàn viên" : author.fullName()));
        return toResponse(saved, authors, currentUser);
    }

    @Transactional
    public CommentResponse editComment(String postId, String commentId, String content, CurrentUser currentUser) {
        getPublishedPost(postId, currentUser);
        PostComment comment = getComment(postId, commentId);
        if (!comment.getMemberId().equals(currentUser.memberId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "COMMENT_FORBIDDEN", "Only the author can edit a comment");
        }
        comment.setContent(content.trim());
        commentRepository.flush();
        return toResponse(comment, memberRoster.cards(List.of(comment.getMemberId())), currentUser);
    }

    @Transactional
    public void deleteComment(String postId, String commentId, CurrentUser currentUser) {
        getReadablePost(postId, currentUser);
        PostComment comment = getComment(postId, commentId);
        if (!canDelete(comment, currentUser)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "COMMENT_FORBIDDEN", "Only the author or a ward officer can delete a comment");
        }
        comment.markDeleted();
    }

    @Transactional
    public EngagementResponse react(String postId, ReactionType type, CurrentUser currentUser) {
        getPublishedPost(postId, currentUser);
        String memberId = requireMemberId(currentUser);
        PostReaction reaction = reactionRepository.findById(new PostReaction.Key(postId, memberId))
                .orElseGet(() -> new PostReaction(postId, memberId, type));
        reaction.setReaction(type);
        reactionRepository.saveAndFlush(reaction);
        return engagementOf(List.of(postId), currentUser).get(0);
    }

    @Transactional
    public EngagementResponse unreact(String postId, CurrentUser currentUser) {
        getReadablePost(postId, currentUser);
        reactionRepository.findById(new PostReaction.Key(postId, requireMemberId(currentUser)))
                .ifPresent(reaction -> {
                    reactionRepository.delete(reaction);
                    reactionRepository.flush();
                });
        return engagementOf(List.of(postId), currentUser).get(0);
    }

    /** Counts for many posts in three queries; posts the caller cannot read are left out. */
    @Transactional(readOnly = true)
    public List<EngagementResponse> engagement(Collection<String> postIds, CurrentUser currentUser) {
        List<String> readable = postRepository.findAllById(new LinkedHashSet<>(postIds)).stream()
                .filter(post -> postScopeService.canRead(currentUser, post))
                .map(Post::getId)
                .toList();
        return engagementOf(readable, currentUser);
    }

    private List<EngagementResponse> engagementOf(List<String> postIds, CurrentUser currentUser) {
        if (postIds.isEmpty()) {
            return List.of();
        }
        Map<String, Map<ReactionType, Long>> counts = new HashMap<>();
        for (Object[] row : reactionRepository.countGroupedByPostAndType(postIds)) {
            counts.computeIfAbsent((String) row[0], key -> new EnumMap<>(ReactionType.class))
                    .put((ReactionType) row[1], ((Number) row[2]).longValue());
        }
        Map<String, ReactionType> mine = new HashMap<>();
        if (currentUser.memberId() != null) {
            reactionRepository.findMine(postIds, currentUser.memberId())
                    .forEach(reaction -> mine.put(reaction.getId().getPostId(), reaction.getReaction()));
        }
        Map<String, Long> comments = commentRepository.counts(postIds);
        return postIds.stream().map(postId -> {
            Map<ReactionType, Long> byType = counts.getOrDefault(postId, new EnumMap<>(ReactionType.class));
            return new EngagementResponse(
                    postId,
                    byType,
                    byType.values().stream().mapToLong(Long::longValue).sum(),
                    mine.get(postId),
                    comments.getOrDefault(postId, 0L)
            );
        }).toList();
    }

    private CommentResponse toResponse(PostComment comment, Map<String, MemberRoster.Card> authors, CurrentUser currentUser) {
        MemberRoster.Card author = authors.get(comment.getMemberId());
        return new CommentResponse(
                comment.getId(),
                comment.getPostId(),
                comment.getMemberId(),
                author == null ? null : author.fullName(),
                author == null ? null : author.avatarUrl(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                comment.getUpdatedAt() != null && comment.getUpdatedAt().isAfter(comment.getCreatedAt().plusSeconds(1)),
                comment.getMemberId().equals(currentUser.memberId()),
                canDelete(comment, currentUser)
        );
    }

    private static boolean canDelete(PostComment comment, CurrentUser currentUser) {
        return comment.getMemberId().equals(currentUser.memberId()) || currentUser.hasWardScope();
    }

    private Post getReadablePost(String postId, CurrentUser currentUser) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "Post not found"));
        postScopeService.requireRead(currentUser, post);
        return post;
    }

    private Post getPublishedPost(String postId, CurrentUser currentUser) {
        Post post = getReadablePost(postId, currentUser);
        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new ApiException(HttpStatus.CONFLICT, "POST_NOT_PUBLISHED", "Post is not published");
        }
        return post;
    }

    private PostComment getComment(String postId, String commentId) {
        return commentRepository.findByIdAndPostId(commentId, postId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "Comment not found"));
    }

    private static String requireMemberId(CurrentUser currentUser) {
        if (currentUser.memberId() == null || currentUser.memberId().isBlank()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "MEMBER_CONTEXT_REQUIRED", "Current user is not linked to a member profile");
        }
        return currentUser.memberId();
    }
}
