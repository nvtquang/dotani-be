package com.dotani.member;

import com.dotani.support.IntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dotani.member.entity.Member;
import com.dotani.member.entity.MemberRole;
import com.dotani.member.entity.MemberStatus;
import com.dotani.member.entity.OrganizationUnit;
import com.dotani.member.entity.OrganizationUnitType;
import com.dotani.member.repository.MemberRepository;
import com.dotani.member.repository.MemberRoleChangeAuditRepository;
import com.dotani.member.repository.OrganizationUnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class MemberDashboardIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationUnitRepository organizationUnitRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberRoleChangeAuditRepository auditRepository;

    private OrganizationUnit ward;
    private OrganizationUnit tdp1;
    private OrganizationUnit tdp2;

    @BeforeEach
    void setUp() {
        auditRepository.deleteAll();
        memberRepository.deleteAll();
        organizationUnitRepository.deleteAll();

        ward = saveOrganization("Phuong Thuong Cat", "THUONG_CAT", OrganizationUnitType.WARD, null);
        tdp1 = saveOrganization("Chi doan TDP 1", "TDP_1", OrganizationUnitType.YOUTH_UNION_BRANCH, ward);
        tdp2 = saveOrganization("Chi doan TDP 2", "TDP_2", OrganizationUnitType.YOUTH_UNION_BRANCH, ward);

        saveMember("Ward Secretary", "ward-secretary", tdp1, MemberRole.WARD_SECRETARY, MemberStatus.ACTIVE);
        saveMember("TDP 1 Secretary", "tdp1-secretary", tdp1, MemberRole.TDP_SECRETARY, MemberStatus.ACTIVE);
        saveMember("TDP 1 Member", "tdp1-member", tdp1, MemberRole.MEMBER, MemberStatus.INACTIVE);
        saveMember("TDP 2 Member", "tdp2-member", tdp2, MemberRole.MEMBER, MemberStatus.ACTIVE);
    }

    @Test
    void wardSecretarySeesWardDashboardCounts() throws Exception {
        mockMvc.perform(withWardSecretary(get("/api/dashboard/member-summary")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMembers").value(4))
                .andExpect(jsonPath("$.officerCount").value(2))
                .andExpect(jsonPath("$.membersByTdp.length()").value(2))
                .andExpect(jsonPath("$.memberStatusCounts.ACTIVE").value(3))
                .andExpect(jsonPath("$.memberStatusCounts.INACTIVE").value(1))
                .andExpect(jsonPath("$.membersByTdp[?(@.organizationName == 'Chi doan TDP 1' && @.count == 3)]").exists())
                .andExpect(jsonPath("$.membersByTdp[?(@.organizationName == 'Chi doan TDP 2' && @.count == 1)]").exists());
    }

    @Test
    void tdpSecretaryOnlySeesOwnTdpDashboardCounts() throws Exception {
        mockMvc.perform(withTdpSecretary(get("/api/dashboard/member-summary"), tdp1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMembers").value(3))
                .andExpect(jsonPath("$.officerCount").value(2))
                .andExpect(jsonPath("$.membersByTdp.length()").value(1))
                .andExpect(jsonPath("$.membersByTdp[0].organizationId").value(tdp1.getId()))
                .andExpect(jsonPath("$.memberStatusCounts.ACTIVE").value(2))
                .andExpect(jsonPath("$.memberStatusCounts.INACTIVE").value(1));
    }

    private OrganizationUnit saveOrganization(
            String name,
            String code,
            OrganizationUnitType type,
            OrganizationUnit parent
    ) {
        OrganizationUnit organizationUnit = new OrganizationUnit();
        organizationUnit.setName(name);
        organizationUnit.setCode(code);
        organizationUnit.setType(type);
        organizationUnit.setParent(parent);
        return organizationUnitRepository.save(organizationUnit);
    }

    private void saveMember(
            String fullName,
            String userId,
            OrganizationUnit organization,
            MemberRole role,
            MemberStatus status
    ) {
        Member member = new Member();
        member.setFullName(fullName);
        member.setUserId(userId);
        member.setEmail(userId + "@example.com");
        member.setMemberRole(role);
        member.setMemberStatus(status);
        member.setOrganization(organization);
        memberRepository.save(member);
    }

    private MockHttpServletRequestBuilder withWardSecretary(MockHttpServletRequestBuilder request) {
        return request
                .with(auth.identity("ward-secretary-user", "WARD_SECRETARY", ward.getId(), null, null));
    }

    private MockHttpServletRequestBuilder withTdpSecretary(
            MockHttpServletRequestBuilder request,
            String tdpId
    ) {
        return request
                .with(auth.identity("tdp-secretary-user", "TDP_SECRETARY", ward.getId(), tdpId, null));
    }
}
