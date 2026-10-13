package com.dotani.content.controller;

import com.dotani.content.dto.PostEngagementDtos.CommentRequest;
import com.dotani.content.dto.PostEngagementDtos.CommentResponse;
import com.dotani.content.dto.PostEngagementDtos.EngagementLookupRequest;
import com.dotani.content.dto.PostEngagementDtos.EngagementResponse;
import com.dotani.content.dto.PostEngagementDtos.ReactionRequest;
import com.dotani.content.service.PostEngagementService;
import com.dotani.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
@Tag(name = "Post engagement", description = "Comments and reactions on published posts.")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT Bearer token"),
        @ApiResponse(responseCode = "403", description = "Post out of scope, or not the comment's author"),
        @ApiResponse(responseCode = "404", description = "Post or comment not found"),
        @ApiResponse(responseCode = "409", description = "Post is not published")
})
public class PostEngagementController {

    private final PostEngagementService engagementService;

    public PostEngagementController(PostEngagementService engagementService) {
        this.engagementService = engagementService;
    }

    @PostMapping("/engagement")
    @Operation(summary = "Reactions and comment counts of many posts", description = "One request for a page of the feed. Posts the caller cannot read are left out.")
    public List<EngagementResponse> engagement(
            @Valid @RequestBody EngagementLookupRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return engagementService.engagement(request.postIds(), currentUser);
    }

    @GetMapping("/{id}/comments")
    @Operation(summary = "List comments", description = "Oldest first.")
    public Page<CommentResponse> comments(
            @PathVariable("id") String id,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return engagementService.comments(id, pageable, currentUser);
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a comment", description = "Notifies the post's author.")
    public CommentResponse addComment(
            @PathVariable("id") String id,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return engagementService.addComment(id, request.content(), currentUser);
    }

    @PutMapping("/{id}/comments/{commentId}")
    @Operation(summary = "Edit my comment")
    public CommentResponse editComment(
            @PathVariable("id") String id,
            @PathVariable("commentId") String commentId,
            @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return engagementService.editComment(id, commentId, request.content(), currentUser);
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a comment", description = "The author, or a ward officer moderating.")
    public void deleteComment(
            @PathVariable("id") String id,
            @PathVariable("commentId") String commentId,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        engagementService.deleteComment(id, commentId, currentUser);
    }

    @PutMapping("/{id}/reaction")
    @Operation(summary = "React to a post", description = "Sets or changes the caller's reaction.")
    public EngagementResponse react(
            @PathVariable("id") String id,
            @Valid @RequestBody ReactionRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return engagementService.react(id, request.type(), currentUser);
    }

    @DeleteMapping("/{id}/reaction")
    @Operation(summary = "Remove my reaction")
    public EngagementResponse unreact(@PathVariable("id") String id, @AuthenticationPrincipal CurrentUser currentUser) {
        return engagementService.unreact(id, currentUser);
    }
}
