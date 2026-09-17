package com.hcmcyu.event.controller;

import com.hcmcyu.event.dto.EventDashboardSummaryResponse;
import com.hcmcyu.event.security.CurrentUser;
import com.hcmcyu.event.service.EventDashboardService;
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
@Tag(name = "Dashboard", description = "Event-service dashboard summary APIs used by api-gateway aggregation.")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT Bearer token"),
        @ApiResponse(responseCode = "403", description = "Role or organization scope denied")
})
public class EventDashboardController {

    private final EventDashboardService eventDashboardService;

    public EventDashboardController(EventDashboardService eventDashboardService) {
        this.eventDashboardService = eventDashboardService;
    }

    @GetMapping("/event-summary")
    @Operation(summary = "Get event dashboard summary", description = "WARD officers see ward counts. TDP officers see only their TDP. MEMBER receives member-safe event summary.")
    public EventDashboardSummaryResponse getSummary(@AuthenticationPrincipal CurrentUser currentUser) {
        return eventDashboardService.getSummary(currentUser);
    }
}
