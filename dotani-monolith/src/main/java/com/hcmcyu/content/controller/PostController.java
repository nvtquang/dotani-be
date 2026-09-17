package com.hcmcyu.content.controller;

import com.hcmcyu.content.dto.PostRequest;
import com.hcmcyu.content.dto.PostResponse;
import com.hcmcyu.content.entity.PostType;
import com.hcmcyu.content.security.CurrentUser;
import com.hcmcyu.content.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/posts")
@Tag(name = "Posts", description = "News, announcements, activity reports, and post image APIs.")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation error or invalid upload"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT Bearer token"),
        @ApiResponse(responseCode = "403", description = "Role or organization scope denied"),
        @ApiResponse(responseCode = "404", description = "Post or image not found")
})
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    @Operation(summary = "List posts", description = "Supports organization, type, date, page, and size filters. MEMBER sees published posts allowed by scope.")
    public Page<PostResponse> findAll(
            @RequestParam(required = false, name = "organization") String organizationId,
            @RequestParam(required = false, name = "type") PostType type,
            @RequestParam(required = false, name = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Pageable pageable,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return postService.findAll(organizationId, type, date, pageable, currentUser);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get post by id", description = "Returns a post visible to the current user. Organization scope is enforced.")
    public PostResponse findById(
            @PathVariable("id") String id,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return postService.findById(id, currentUser);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create post", description = "WARD officers can manage ward posts. TDP officers can create posts for their own TDP.")
    public PostResponse create(
            @Valid @RequestBody PostRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return postService.create(request, currentUser);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update post", description = "Enforces role and organization scope. TDP officers cannot edit another TDP's post.")
    public PostResponse update(
            @PathVariable("id") String id,
            @Valid @RequestBody PostRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return postService.update(id, request, currentUser);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete post", description = "Enforces role and organization scope.")
    public void delete(
            @PathVariable("id") String id,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        postService.delete(id, currentUser);
    }

    @PostMapping("/{id}/images")
    @Operation(summary = "Upload post images", description = "multipart/form-data upload of multiple images using StorageService. File validation is enforced server-side.")
    public PostResponse uploadImages(
            @PathVariable("id") String id,
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return postService.uploadImages(id, files, currentUser);
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete post image", description = "Deletes a post image when the current user can manage the post.")
    public void deleteImage(
            @PathVariable("id") String id,
            @PathVariable("imageId") String imageId,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        postService.deleteImage(id, imageId, currentUser);
    }
}
