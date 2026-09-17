package com.hcmcyu.member.controller;

import com.hcmcyu.member.dto.MemberDashboardSummaryResponse;
import com.hcmcyu.member.security.CurrentUser;
import com.hcmcyu.member.service.MemberDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Member-service dashboard summary APIs used by api-gateway aggregation.")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT Bearer token"),
        @ApiResponse(responseCode = "403", description = "Role or organization scope denied")
})
public class MemberDashboardController {

    private final MemberDashboardService memberDashboardService;

    public MemberDashboardController(MemberDashboardService memberDashboardService) {
        this.memberDashboardService = memberDashboardService;
    }

    @GetMapping("/member-summary")
    @Operation(summary = "Get member dashboard summary", description = "WARD officers see ward statistics. TDP officers see only their TDP. MEMBER receives member-safe summary data.")
    public MemberDashboardSummaryResponse getSummary(@AuthenticationPrincipal CurrentUser currentUser) {
        return memberDashboardService.getSummary(currentUser);
    }
}
