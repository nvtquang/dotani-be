package com.dotani.report.controller;

import com.dotani.report.dto.ReportDtos.MemberReport;
import com.dotani.report.dto.ReportDtos.ParticipationReport;
import com.dotani.report.dto.ReportDtos.PeriodGranularity;
import com.dotani.report.service.ReportService;
import com.dotani.shared.security.CurrentUser;
import com.dotani.shared.time.AppTime;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports", description = "Báo cáo thống kê: đoàn viên theo TDP, độ tuổi, giới tính; tỷ lệ tham gia sự kiện theo tháng/quý.")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Invalid year"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT Bearer token"),
        @ApiResponse(responseCode = "403", description = "Members cannot see reports; TDP officers only their TDP"),
        @ApiResponse(responseCode = "404", description = "Organization not found")
})
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/members")
    @Operation(summary = "Membership statistics", description = "Members per TDP, status, gender and age group. organizationId narrows a ward report to one TDP.")
    public MemberReport members(
            @RequestParam(name = "organizationId", required = false) String organizationId,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return reportService.members(organizationId, currentUser);
    }

    @GetMapping("/participation")
    @Operation(summary = "Event participation", description = "Invited and registered members per month or quarter of a year, per TDP and per event.")
    public ParticipationReport participation(
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "granularity", defaultValue = "MONTH") PeriodGranularity granularity,
            @RequestParam(name = "organizationId", required = false) String organizationId,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return reportService.participation(year == null ? AppTime.today().getYear() : year, granularity, organizationId, currentUser);
    }
}
