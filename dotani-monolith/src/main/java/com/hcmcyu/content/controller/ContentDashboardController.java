package com.hcmcyu.content.controller;

import com.hcmcyu.content.dto.ContentDashboardSummaryResponse;
import com.hcmcyu.content.security.CurrentUser;
import com.hcmcyu.content.service.ContentDashboardService;
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
@Tag(name = "Dashboard", description = "Content-service dashboard summary APIs used by api-gateway aggregation.")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT Bearer token"),
        @ApiResponse(responseCode = "403", description = "Role or organization scope denied")
})
public class ContentDashboardController {

    private final ContentDashboardService contentDashboardService;

    public ContentDashboardController(ContentDashboardService contentDashboardService) {
        this.contentDashboardService = contentDashboardService;
    }

    @GetMapping("/content-summary")
    @Operation(summary = "Get content dashboard summary", description = "Returns recent activity report and new post summaries scoped to the current user.")
    public ContentDashboardSummaryResponse getSummary(@AuthenticationPrincipal CurrentUser currentUser) {
        return contentDashboardService.getSummary(currentUser);
    }
}
