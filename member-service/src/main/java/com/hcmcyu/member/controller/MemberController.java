package com.hcmcyu.member.controller;

import com.hcmcyu.member.dto.MemberBankingRequest;
import com.hcmcyu.member.dto.MemberBankingResponse;
import com.hcmcyu.member.dto.MemberDirectoryResponse;
import com.hcmcyu.member.dto.MemberProfileUpdateRequest;
import com.hcmcyu.member.dto.MemberRequest;
import com.hcmcyu.member.dto.MemberResponse;
import com.hcmcyu.member.dto.MemberRoleUpdateRequest;
import com.hcmcyu.member.entity.MemberRole;
import com.hcmcyu.member.entity.MemberStatus;
import com.hcmcyu.member.security.CurrentUser;
import com.hcmcyu.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/members")
@Tag(name = "Members", description = "Member management, personal profile, avatar, and banking QR APIs.")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation error or invalid upload"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT Bearer token"),
        @ApiResponse(responseCode = "403", description = "Role or organization scope denied"),
        @ApiResponse(responseCode = "404", description = "Member not found"),
        @ApiResponse(responseCode = "409", description = "Business rule conflict")
})
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    @Operation(summary = "List members", description = "WARD officers can list the ward. TDP officers can list only their TDP. MEMBER cannot enumerate other members. Banking/QR fields are never included.")
    public Page<MemberResponse> findAll(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "organizationId", required = false) String organizationId,
            @RequestParam(name = "status", required = false) MemberStatus status,
            @RequestParam(name = "role", required = false) MemberRole role,
            @PageableDefault(size = 20, sort = "fullName") Pageable pageable,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return memberService.findAll(keyword, organizationId, status, role, pageable, currentUser);
    }

    @GetMapping("/directory")
    @Operation(summary = "Search member directory", description = "Authenticated lightweight directory for chat member picking. Returns only id, name, organization, and avatar; banking/QR and sensitive profile fields are never included.")
    public Page<MemberDirectoryResponse> directory(
            @RequestParam(name = "keyword", required = false) String keyword,
            @PageableDefault(size = 10, sort = "fullName") Pageable pageable
    ) {
        return memberService.findDirectory(keyword, pageable);
    }

    @GetMapping("/me")
    @Operation(summary = "Get my profile", description = "Returns the current member profile from JWT identity.")
    public MemberResponse me(@AuthenticationPrincipal CurrentUser currentUser) {
        return memberService.findCurrentProfile(currentUser);
    }

    @PutMapping("/me")
    @Operation(summary = "Update my profile", description = "Member can update only allowed personal fields. Role, organizationId, memberStatus, joinDate, and permission fields are ignored/not accepted.")
    public MemberResponse updateMe(
            @Valid @RequestBody MemberProfileUpdateRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return memberService.updateCurrentProfile(request, currentUser);
    }

    @PostMapping("/me/avatar")
    @Operation(summary = "Upload my avatar", description = "multipart/form-data image upload. Allowed extensions: jpg, jpeg, png, webp. Server generates a safe storage filename.")
    public MemberResponse uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return memberService.uploadCurrentAvatar(file, currentUser);
    }

    @DeleteMapping("/me/avatar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete my avatar", description = "Deletes the current member avatar reference and stored file when available.")
    public void deleteAvatar(@AuthenticationPrincipal CurrentUser currentUser) {
        memberService.deleteCurrentAvatar(currentUser);
    }

    @GetMapping("/me/banking")
    @Operation(summary = "Get my banking QR profile", description = "Returns banking metadata and QR URL for the current member only. This is personal profile data, not payment processing.")
    public MemberBankingResponse myBanking(@AuthenticationPrincipal CurrentUser currentUser) {
        return memberService.findCurrentBanking(currentUser);
    }

    @PutMapping("/me/banking")
    @Operation(summary = "Update my banking metadata", description = "Updates personal banking metadata. No payment transaction or payment gateway integration is performed.")
    public MemberBankingResponse updateMyBanking(
            @Valid @RequestBody MemberBankingRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return memberService.updateCurrentBanking(request, currentUser);
    }

    @PostMapping("/me/banking/qr")
    @Operation(summary = "Upload my banking QR image", description = "multipart/form-data image upload using the shared StorageService validation and safe filename generation.")
    public MemberBankingResponse uploadMyBankQr(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return memberService.uploadCurrentBankQr(file, currentUser);
    }

    @DeleteMapping("/me/banking/qr")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete my banking QR image", description = "Deletes the current member banking QR image reference and stored file when available.")
    public void deleteMyBankQr(@AuthenticationPrincipal CurrentUser currentUser) {
        memberService.deleteCurrentBankQr(currentUser);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get member by id", description = "Enforces organization scope to prevent IDOR/BOLA. TDP officers cannot access members from another TDP.")
    public MemberResponse findById(
            @PathVariable("id") String id,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return memberService.findById(id, currentUser);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create member", description = "WARD officers can create in the ward. TDP officers can create only in their own TDP. MEMBER is forbidden.")
    public MemberResponse create(
            @Valid @RequestBody MemberRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return memberService.create(request, currentUser);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update member", description = "Enforces role and organization scope. TDP officers cannot update members outside their TDP or change protected role fields.")
    public MemberResponse update(
            @PathVariable("id") String id,
            @Valid @RequestBody MemberRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return memberService.update(id, request, currentUser);
    }

    @PutMapping("/{memberId}/role")
    @Operation(summary = "Change member role", description = "Only WARD_SECRETARY can assign roles. WARD_DEPUTY_SECRETARY, TDP officers, and MEMBER receive 403. Role changes are audited and synchronized to auth-service.")
    public MemberResponse updateRole(
            @PathVariable("memberId") String memberId,
            @Valid @RequestBody MemberRoleUpdateRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return memberService.updateRole(memberId, request, currentUser);
    }

    @GetMapping("/{memberId}/banking")
    @Operation(summary = "Get member banking QR by scope", description = "MEMBER can access only self. TDP officers can access members in their TDP. WARD officers can access the ward.")
    public MemberBankingResponse findBankingByMemberId(
            @PathVariable("memberId") String memberId,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return memberService.findBankingByMemberId(memberId, currentUser);
    }

    @GetMapping("/{memberId}/banking/qr-image")
    @Operation(summary = "Get scoped QR Banking image", description = "Returns the QR Banking image only after member/profile scope authorization succeeds.")
    public ResponseEntity<Resource> getBankQrImage(
            @PathVariable("memberId") String memberId,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        Resource resource = memberService.loadBankQrImage(memberId, currentUser);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Disable member", description = "Soft-disables a member. Enforces role and organization scope.")
    public void delete(@PathVariable("id") String id, @AuthenticationPrincipal CurrentUser currentUser) {
        memberService.softDelete(id, currentUser);
    }
}
