package com.dotani.content.service;

import com.dotani.shared.attachment.Attachment;
import com.dotani.shared.events.DomainEvents.PostPublished;
import org.springframework.context.ApplicationEventPublisher;
import com.dotani.shared.attachment.AttachmentKind;
import com.dotani.shared.attachment.AttachmentOwnerType;
import com.dotani.shared.attachment.AttachmentRepository;
import com.dotani.audit.entity.AuditAction;
import com.dotani.audit.entity.ResourceType;
import com.dotani.audit.service.AuditDiff;
import com.dotani.audit.service.AuditLogService;

import com.dotani.content.dto.PostRequest;
import com.dotani.content.dto.PostResponse;
import com.dotani.content.entity.Post;
import com.dotani.content.entity.PostStatus;
import com.dotani.content.entity.PostType;
import com.dotani.shared.exception.ApiException;
import com.dotani.content.mapper.PostMapper;
import com.dotani.content.repository.PostRepository;
import com.dotani.content.repository.PostSpecifications;
import com.dotani.shared.security.CurrentUser;
import java.time.LocalDate;
import java.util.ArrayList;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final AttachmentRepository attachmentRepository;
    private final PostMapper postMapper;
    private final PostScopeService postScopeService;
    private final StorageService storageService;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher events;

    public PostService(
            PostRepository postRepository,
            AttachmentRepository attachmentRepository,
            PostMapper postMapper,
            PostScopeService postScopeService,
            StorageService storageService,
            AuditLogService auditLogService,
            ApplicationEventPublisher events
    ) {
        this.postRepository = postRepository;
        this.attachmentRepository = attachmentRepository;
        this.postMapper = postMapper;
        this.postScopeService = postScopeService;
        this.storageService = storageService;
        this.auditLogService = auditLogService;
        this.events = events;
    }

    /** Newest posts before the cursor, for the mixed feed. */
    @Transactional(readOnly = true)
    public List<PostResponse> findCreatedBefore(Instant before, int size, CurrentUser currentUser) {
        Specification<Post> specification = Specification
                .where(PostSpecifications.visibleTo(currentUser))
                .and(PostSpecifications.createdBefore(before));
        return postMapper.toResponsePage(postRepository.findAll(
                specification, PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")))).getContent();
    }

    @Transactional(readOnly = true)
    public List<PostResponse> searchByTitle(String text, int size, CurrentUser currentUser) {
        Specification<Post> specification = Specification
                .where(PostSpecifications.visibleTo(currentUser))
                .and(PostSpecifications.titleContains(text));
        return postMapper.toResponsePage(postRepository.findAll(
                specification, PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "createdAt")))).getContent();
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> findAll(
            String organizationId,
            PostType type,
            LocalDate date,
            Pageable pageable,
            CurrentUser currentUser
    ) {
        Specification<Post> specification = Specification
                .where(PostSpecifications.visibleTo(currentUser))
                .and(PostSpecifications.hasOrganization(organizationId))
                .and(PostSpecifications.hasType(type))
                .and(PostSpecifications.onDate(date));

        return postMapper.toResponsePage(postRepository.findAll(specification, pageable));
    }

    @Transactional(readOnly = true)
    public PostResponse findById(String id, CurrentUser currentUser) {
        Post post = getPost(id);
        postScopeService.requireRead(currentUser, post);
        return postMapper.toResponse(post);
    }

    @Transactional
    public PostResponse create(PostRequest request, CurrentUser currentUser) {
        postScopeService.requireManageRequest(currentUser, request.organizationId(), request.type());
        Post post = new Post();
        postMapper.apply(post, request);
        post.setAuthorId(currentUser.memberId() == null || currentUser.memberId().isBlank()
                ? currentUser.userId()
                : currentUser.memberId());
        Post saved = postRepository.save(post);
        auditLogService.record(currentUser, AuditAction.CREATE_POST, ResourceType.POST, saved.getId(),
                saved.getOrganizationId(), AuditDiff.created(auditSnapshot(saved)));
        if (saved.getStatus() == PostStatus.PUBLISHED) {
            events.publishEvent(new PostPublished(saved.getId(), saved.getTitle(), saved.getOrganizationId(), saved.getAuthorId()));
        }
        return postMapper.toResponse(saved);
    }

    @Transactional
    public PostResponse update(String id, PostRequest request, CurrentUser currentUser) {
        Post post = getPost(id);
        postScopeService.requireManage(currentUser, post);
        postScopeService.requireManageRequest(currentUser, request.organizationId(), request.type());
        Map<String, Object> before = auditSnapshot(post);
        PostStatus oldStatus = post.getStatus();
        postMapper.apply(post, request);
        if (oldStatus != PostStatus.PUBLISHED && post.getStatus() == PostStatus.PUBLISHED) {
            events.publishEvent(new PostPublished(post.getId(), post.getTitle(), post.getOrganizationId(), post.getAuthorId()));
        }
        auditLogService.record(currentUser, AuditAction.UPDATE_POST, ResourceType.POST, post.getId(),
                post.getOrganizationId(), AuditDiff.changes(before, auditSnapshot(post)));
        return postMapper.toResponse(post);
    }

    @Transactional
    public void delete(String id, CurrentUser currentUser) {
        Post post = getPost(id);
        postScopeService.requireManage(currentUser, post);
        auditLogService.record(currentUser, AuditAction.DELETE_POST, ResourceType.POST, post.getId(),
                post.getOrganizationId(), AuditDiff.deleted(auditSnapshot(post)));
        // soft delete: the post and its attachments stay in the database
        post.markDeleted();
    }

    @Transactional
    public PostResponse uploadImages(String id, List<MultipartFile> files, CurrentUser currentUser) {
        Post post = getPost(id);
        postScopeService.requireManage(currentUser, post);
        if (files == null || files.isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "POST_IMAGE_REQUIRED",
                    "At least one image is required"
            );
        }

        List<String> storedUrls = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                String imageUrl = storageService.storePostImage(file);
                storedUrls.add(imageUrl);
                post.addAttachment(Attachment.of(
                        AttachmentOwnerType.POST,
                        post.getId(),
                        AttachmentKind.valueOf(storageService.attachmentKind(file)),
                        imageUrl,
                        file.getOriginalFilename(),
                        file.getContentType(),
                        file.getSize(),
                        currentUser.memberId()
                ));
            }
            return postMapper.toResponse(postRepository.saveAndFlush(post));
        } catch (RuntimeException exception) {
            storedUrls.forEach(storageService::delete);
            throw exception;
        }
    }

    @Transactional
    public void deleteImage(String postId, String imageId, CurrentUser currentUser) {
        Post post = getPost(postId);
        postScopeService.requireManage(currentUser, post);
        Attachment image = attachmentRepository.findByIdAndOwnerTypeAndOwnerId(imageId, AttachmentOwnerType.POST, postId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "POST_IMAGE_NOT_FOUND",
                        "Post image not found"
                ));
        String imageUrl = image.getUrl();
        post.removeAttachment(image);
        storageService.delete(imageUrl);
    }

    private Post getPost(String id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "POST_NOT_FOUND",
                        "Post not found"
                ));
    }

    private Map<String, Object> auditSnapshot(Post post) {
        return AuditDiff.snapshot(
                "title", post.getTitle(),
                "type", post.getType(),
                "status", post.getStatus(),
                "organizationId", post.getOrganizationId()
        );
    }
}
