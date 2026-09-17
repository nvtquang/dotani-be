package com.hcmcyu.member.dto;

import com.hcmcyu.member.entity.MemberStatus;
import java.util.List;
import java.util.Map;

public record MemberDashboardSummaryResponse(
        long totalMembers,
        List<OrganizationMemberCountResponse> membersByTdp,
        long officerCount,
        Map<MemberStatus, Long> memberStatusCounts
) {
}
