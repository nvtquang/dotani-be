package com.dotani.content.service;

import com.dotani.audit.entity.AuditAction;
import com.dotani.audit.entity.ResourceType;
import com.dotani.audit.service.AuditLogService;

import com.dotani.content.dto.PostRequest;
import com.dotani.content.dto.PostResponse;
import com.dotani.content.entity.Post;
import com.dotani.content.entity.PostImage;
import com.dotani.content.entity.PostType;
import com.dotani.shared.exception.ApiException;
import com.dotani.content.mapper.PostMapper;
import com.dotani.content.repository.PostImageRepository;
import com.dotani.content.repository.PostRepository;
import com.dotani.content.repository.PostSpecifications;
import com.dotani.shared.security.CurrentUser;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
    private final PostImageRepository postImageRepository;
    private final PostMapper postMapper;
    private final PostScopeService postScopeService;
    private final StorageService storageService;
    private final AuditLogService auditLogService;

    public PostService(
            PostRepository postRepository,
            PostImageRepository postImageRepository,
            PostMapper postMapper,
            PostScopeService postScopeService,
            StorageService storageService,
            AuditLogService auditLogService
    ) {
        this.postRepository = postRepository;
        this.postImageRepository = postImageRepository;
        this.postMapper = postMapper;
        this.postScopeService = postScopeService;
        this.storageService = storageService;
        this.auditLogService = auditLogService;
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

        return postRepository.findAll(specification, pageable).map(postMapper::toResponse);
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
        auditLogService.record(currentUser, AuditAction.CREATE_POST, ResourceType.POST, saved.getId(), saved.getOrganizationId());
        return postMapper.toResponse(saved);
    }

    @Transactional
    public PostResponse update(String id, PostRequest request, CurrentUser currentUser) {
        Post post = getPost(id);
        postScopeService.requireManage(currentUser, post);
        postScopeService.requireManageRequest(currentUser, request.organizationId(), request.type());
        postMapper.apply(post, request);
        auditLogService.record(currentUser, AuditAction.UPDATE_POST, ResourceType.POST, post.getId(), post.getOrganizationId());
        return postMapper.toResponse(post);
    }

    @Transactional
    public void delete(String id, CurrentUser currentUser) {
        Post post = getPost(id);
        postScopeService.requireManage(currentUser, post);
        List<String> imageUrls = post.getImages().stream().map(PostImage::getImageUrl).toList();
        auditLogService.record(currentUser, AuditAction.DELETE_POST, ResourceType.POST, post.getId(), post.getOrganizationId());
        postRepository.delete(post);
        imageUrls.forEach(storageService::delete);
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
                PostImage image = new PostImage();
                image.setImageUrl(imageUrl);
                image.setAttachmentKind(storageService.attachmentKind(file));
                image.setFileName(file.getOriginalFilename());
                post.addImage(image);
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
        PostImage image = postImageRepository.findByIdAndPost_Id(imageId, postId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "POST_IMAGE_NOT_FOUND",
                        "Post image not found"
                ));
        String imageUrl = image.getImageUrl();
        post.removeImage(image);
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
}
