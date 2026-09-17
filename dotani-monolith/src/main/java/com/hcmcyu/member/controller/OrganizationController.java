package com.hcmcyu.member.controller;

import com.hcmcyu.member.dto.MemberSummaryResponse;
import com.hcmcyu.member.dto.OrganizationUnitRequest;
import com.hcmcyu.member.dto.OrganizationUnitResponse;
import com.hcmcyu.member.security.CurrentUser;
import com.hcmcyu.member.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/organizations")
@Tag(name = "Organizations", description = "Ward and TDP organization unit APIs.")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT Bearer token"),
        @ApiResponse(responseCode = "403", description = "Role or organization scope denied"),
        @ApiResponse(responseCode = "404", description = "Organization not found")
})
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping
    @Operation(summary = "List organizations", description = "WARD officers can view the ward tree. TDP officers are scoped to their own TDP. MEMBER sees only allowed organization data.")
    public List<OrganizationUnitResponse> findAll(@AuthenticationPrincipal CurrentUser currentUser) {
        return organizationService.findAll(currentUser);
    }

    @GetMapping("/public")
    @Operation(summary = "List public TDP branches", description = "Public read-only endpoint used by register form to choose a TDP.")
    public List<OrganizationUnitResponse> findPublicBranches() {
        return organizationService.findPublicBranches();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get organization by id", description = "Enforces organization scope; TDP officers cannot access another TDP by id.")
    public OrganizationUnitResponse findById(
            @PathVariable("id") String id,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return organizationService.findById(id, currentUser);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create organization", description = "Administrative endpoint. WARD scope is required for ward-wide organization changes.")
    public OrganizationUnitResponse create(
            @Valid @RequestBody OrganizationUnitRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return organizationService.create(request, currentUser);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update organization", description = "Administrative endpoint with role and organization scope enforcement.")
    public OrganizationUnitResponse update(
            @PathVariable("id") String id,
            @Valid @RequestBody OrganizationUnitRequest request,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return organizationService.update(id, request, currentUser);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete organization", description = "Administrative endpoint with role and organization scope enforcement.")
    public void delete(@PathVariable("id") String id, @AuthenticationPrincipal CurrentUser currentUser) {
        organizationService.delete(id, currentUser);
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "List members in organization", description = "Returns member summaries for an organization visible to the current user.")
    public List<MemberSummaryResponse> findMembers(
            @PathVariable("id") String id,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return organizationService.findMembers(id, currentUser);
    }
}
