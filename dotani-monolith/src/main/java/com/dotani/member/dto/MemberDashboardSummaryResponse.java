package com.dotani.member.dto;

import com.dotani.member.entity.MemberStatus;
import java.util.List;
import java.util.Map;

public record MemberDashboardSummaryResponse(
        long totalMembers,
        List<OrganizationMemberCountResponse> membersByTdp,
        long officerCount,
        Map<MemberStatus, Long> memberStatusCounts
) {
}
