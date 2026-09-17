package com.hcmcyu.content.dto;

import java.util.List;

public record ContentDashboardSummaryResponse(
        long recentActivityReportCount,
        List<PostResponse> newPosts
) {
}
