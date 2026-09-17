package com.hcmcyu.member.service;

import com.hcmcyu.member.dto.MemberDashboardSummaryResponse;
import com.hcmcyu.member.dto.OrganizationMemberCountResponse;
import com.hcmcyu.member.entity.MemberRole;
import com.hcmcyu.member.entity.MemberStatus;
import com.hcmcyu.member.entity.OrganizationUnit;
import com.hcmcyu.member.entity.OrganizationUnitType;
import com.hcmcyu.member.repository.MemberRepository;
import com.hcmcyu.member.repository.MemberSpecifications;
import com.hcmcyu.member.repository.OrganizationUnitRepository;
import com.hcmcyu.member.security.CurrentUser;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberDashboardService {

    private static final List<MemberRole> OFFICER_ROLES = List.of(
            MemberRole.WARD_SECRETARY,
            MemberRole.WARD_DEPUTY_SECRETARY,
            MemberRole.TDP_SECRETARY,
            MemberRole.TDP_DEPUTY_SECRETARY
    );

    private final MemberRepository memberRepository;
    private final OrganizationUnitRepository organizationUnitRepository;

    public MemberDashboardService(
            MemberRepository memberRepository,
            OrganizationUnitRepository organizationUnitRepository
    ) {
        this.memberRepository = memberRepository;
        this.organizationUnitRepository = organizationUnitRepository;
    }

    @Transactional(readOnly = true)
    public MemberDashboardSummaryResponse getSummary(CurrentUser currentUser) {
        String organizationScope = organizationScope(currentUser);
        long totalMembers = currentUser.hasWardScope()
                ? memberRepository.count()
                : memberRepository.count(MemberSpecifications.withinScope(currentUser));

        List<OrganizationMemberCountResponse> membersByTdp = currentUser.hasWardScope()
                ? organizationUnitRepository.findByType(OrganizationUnitType.YOUTH_UNION_BRANCH)
                        .stream()
                        .map(this::toOrganizationMemberCount)
                        .toList()
                : organizationUnitRepository.findById(organizationScope)
                        .map(organization -> List.of(toOrganizationMemberCount(organization)))
                        .orElseGet(List::of);

        long officerCount = currentUser.hasWardScope()
                ? memberRepository.countByMemberRoleIn(OFFICER_ROLES)
                : memberRepository.countByOrganization_IdAndMemberRoleIn(organizationScope, OFFICER_ROLES);

        return new MemberDashboardSummaryResponse(
                totalMembers,
                membersByTdp,
                officerCount,
                statusCounts(organizationScope)
        );
    }

    private String organizationScope(CurrentUser currentUser) {
        if (currentUser.hasWardScope()) {
            return null;
        }
        if (currentUser.tdpId() != null && !currentUser.tdpId().isBlank()) {
            return currentUser.tdpId();
        }
        return currentUser.organizationId();
    }

    private OrganizationMemberCountResponse toOrganizationMemberCount(OrganizationUnit organization) {
        return new OrganizationMemberCountResponse(
                organization.getId(),
                organization.getName(),
                memberRepository.countByOrganization_Id(organization.getId())
        );
    }

    private Map<MemberStatus, Long> statusCounts(String organizationId) {
        Map<MemberStatus, Long> counts = new EnumMap<>(MemberStatus.class);
        Arrays.stream(MemberStatus.values()).forEach(status -> counts.put(status, 0L));
        memberRepository.countByStatusScoped(organizationId)
                .forEach(row -> counts.put((MemberStatus) row[0], (Long) row[1]));
        return counts;
    }
}
