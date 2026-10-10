package com.dotani.shared.dashboard;

public record DashboardSummaryResponse(
        Object member,
        Object event,
        Object content,
        Object notification
) {
}
