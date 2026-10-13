package com.dotani.poll.controller;

import com.dotani.poll.dto.PollDtos.PollRequest;
import com.dotani.poll.dto.PollDtos.PollResponse;
import com.dotani.poll.dto.PollDtos.VoteRequest;
import com.dotani.poll.entity.PollStatus;
import com.dotani.poll.service.PollService;
import com.dotani.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/polls")
@Tag(name = "Polls", description = "Khảo sát / biểu quyết: surveys and votes, open or secret ballot, optionally tied to an event (đại hội).")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation error or invalid choice"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT Bearer token"),
        @ApiResponse(responseCode = "403", description = "Role or organization scope denied, or not an eligible voter"),
        @ApiResponse(responseCode = "404", description = "Poll not found"),
        @ApiResponse(responseCode = "409", description = "Poll not open, already voted, or locked")
})
public class PollController {

    private final PollService pollService;

    public PollController(PollService pollService) {
        this.pollService = pollService;
    }

    @GetMapping
    @Operation(summary = "List polls", description = "Polls of the caller's ward and own TDP; members do not see drafts. Filter by status and eventId.")
    public Page<PollResponse> findAll(
            @RequestParam(name = "status", required = false) PollStatus status,
            @RequestParam(name = "eventId", required = false) String eventId,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return pollService.findAll(status, eventId, pageable, currentUser);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get poll", description = "Vote counts are included when the results are visible to the caller.")
    public PollResponse findById(@PathVariable("id") String id, @AuthenticationPrincipal CurrentUser currentUser) {
        return pollService.findById(id, currentUser);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create poll", description = "Draft by default; open=true opens it right away and notifies the members.")
    public PollResponse create(@Valid @RequestBody PollRequest request, @AuthenticationPrincipal CurrentUser currentUser) {
        return pollService.create(request, currentUser);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update poll", description = "Once open, options, number of choices, secret ballot and organization are locked.")
    public PollResponse update(
            @PathVariable("id") String id,
            @Valid @RequestBody PollRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return pollService.update(id, request, currentUser);
    }

    @PostMapping("/{id}/open")
    @Operation(summary = "Open a draft poll")
    public PollResponse open(@PathVariable("id") String id, @AuthenticationPrincipal CurrentUser currentUser) {
        return pollService.open(id, currentUser);
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Close an open poll")
    public PollResponse close(@PathVariable("id") String id, @AuthenticationPrincipal CurrentUser currentUser) {
        return pollService.close(id, currentUser);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete poll")
    public void delete(@PathVariable("id") String id, @AuthenticationPrincipal CurrentUser currentUser) {
        pollService.delete(id, currentUser);
    }

    @PostMapping("/{id}/votes")
    @Operation(summary = "Vote", description = "Active members of the poll's organization, once, with 1..maxChoices options.")
    public PollResponse vote(
            @PathVariable("id") String id,
            @Valid @RequestBody VoteRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return pollService.vote(id, request.optionIds(), currentUser);
    }
}
