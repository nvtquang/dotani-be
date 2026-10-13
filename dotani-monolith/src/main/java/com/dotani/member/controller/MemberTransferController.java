package com.dotani.member.controller;

import com.dotani.member.dto.MemberTransferRequest;
import com.dotani.member.dto.MemberTransferResponse;
import com.dotani.member.dto.TransferDecisionRequest;
import com.dotani.member.entity.TransferStatus;
import com.dotani.member.service.MemberTransferService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/member-transfers")
@Tag(name = "Member transfers", description = "Chuyển sinh hoạt đoàn giữa các TDP: request, approval and history.")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation error or invalid target TDP"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT Bearer token"),
        @ApiResponse(responseCode = "403", description = "Role or organization scope denied"),
        @ApiResponse(responseCode = "404", description = "Transfer or member not found"),
        @ApiResponse(responseCode = "409", description = "A transfer is already pending or was already decided")
})
public class MemberTransferController {

    private final MemberTransferService transferService;

    public MemberTransferController(MemberTransferService transferService) {
        this.transferService = transferService;
    }

    @GetMapping
    @Operation(summary = "List transfers", description = "Ward officers see all, TDP officers those leaving or entering their TDP, members their own. Filter by status and memberId (a member's history).")
    public Page<MemberTransferResponse> findAll(
            @RequestParam(name = "status", required = false) TransferStatus status,
            @RequestParam(name = "memberId", required = false) String memberId,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return transferService.findAll(status, memberId, pageable, currentUser);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transfer")
    public MemberTransferResponse findById(@PathVariable("id") String id, @AuthenticationPrincipal CurrentUser currentUser) {
        return transferService.findById(id, currentUser);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Request a transfer", description = "Members request their own transfer (memberId omitted); officers can propose one for a member they manage.")
    public MemberTransferResponse request(@Valid @RequestBody MemberTransferRequest request, @AuthenticationPrincipal CurrentUser currentUser) {
        return transferService.request(request, currentUser);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a transfer", description = "Ward officers or officers of the receiving TDP. Moves the member; a TDP officer role falls back to MEMBER.")
    public MemberTransferResponse approve(
            @PathVariable("id") String id,
            @Valid @RequestBody(required = false) TransferDecisionRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return transferService.approve(id, request == null ? null : request.note(), currentUser);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a transfer")
    public MemberTransferResponse reject(
            @PathVariable("id") String id,
            @Valid @RequestBody(required = false) TransferDecisionRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return transferService.reject(id, request == null ? null : request.note(), currentUser);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a pending transfer", description = "The member, the requester or a ward officer.")
    public MemberTransferResponse cancel(@PathVariable("id") String id, @AuthenticationPrincipal CurrentUser currentUser) {
        return transferService.cancel(id, currentUser);
    }
}
