package com.dotani.member;

import com.dotani.support.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.dotani.member.entity.Member;
import com.dotani.member.entity.MemberRole;
import com.dotani.member.entity.MemberStatus;
import com.dotani.member.entity.OrganizationUnit;
import com.dotani.member.entity.OrganizationUnitType;
import com.dotani.member.repository.MemberRepository;
import com.dotani.member.repository.MemberRoleChangeAuditRepository;
import com.dotani.member.repository.OrganizationUnitRepository;
import com.dotani.shared.security.Role;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class MemberSecurityRegressionTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationUnitRepository organizationUnitRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberRoleChangeAuditRepository auditRepository;

    private OrganizationUnit ward;
    private OrganizationUnit tdp1;
    private OrganizationUnit tdp2;
    private Member memberA;
    private Member memberB;

    @BeforeEach
    void setUp() {
        auditRepository.deleteAll();
        memberRepository.deleteAll();
        organizationUnitRepository.deleteAll();

        ward = saveOrganization("Phuong Thuong Cat", "THUONG_CAT", OrganizationUnitType.WARD, null);
        tdp1 = saveOrganization("Chi doan TDP 1", "TDP_1", OrganizationUnitType.YOUTH_UNION_BRANCH, ward);
        tdp2 = saveOrganization("Chi doan TDP 2", "TDP_2", OrganizationUnitType.YOUTH_UNION_BRANCH, ward);

        memberA = saveMember("Member A", "member-a-user", tdp1, MemberRole.MEMBER);
        data.account(memberA);
        memberB = saveMember("Member B", "member-b-user", tdp2, MemberRole.MEMBER);
        memberB.setBankName("VCB");
        memberB.setBankCode("VCB");
        memberB.setAccountNumber("1234567890");
        memberB.setAccountHolderName("MEMBER B");
        memberB.setBankQrImageUrl("/uploads/bank-qr/qr-b.png");
        memberRepository.save(memberB);
    }

    @Test
    void tdpSecretaryFromTdp1CannotAccessMemberFromTdp2ById() throws Exception {
        mockMvc.perform(withTdpSecretary(get("/api/members/{id}", memberB.getId()), tdp1.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));

        mockMvc.perform(withTdpSecretary(put("/api/members/{id}", memberB.getId()), tdp1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "userId", memberB.getUserId(),
                                "fullName", "Tampered",
                                "organizationId", tdp2.getId()
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));
    }

    @Test
    void memberACannotAccessMemberBProfileOrQrBanking() throws Exception {
        mockMvc.perform(withMemberA(get("/api/members/{id}", memberB.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));

        mockMvc.perform(withMemberA(get("/api/members/{id}/banking", memberB.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));
    }

    @Test
    void onlyWardSecretaryCanAssignRole() throws Exception {
        mockMvc.perform(withWardDeputySecretary(put("/api/members/{memberId}/role", memberA.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("role", "TDP_SECRETARY"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ROLE_UPDATE_FORBIDDEN"));

        mockMvc.perform(withWardSecretary(put("/api/members/{memberId}/role", memberA.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("role", "TDP_SECRETARY"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberRole").value("TDP_SECRETARY"));

        assertThat(memberRepository.findById(memberA.getId()).orElseThrow().getMemberRole())
                .isEqualTo(MemberRole.TDP_SECRETARY);
        assertThat(data.accountRole(memberA.getUserId())).isEqualTo(Role.TDP_SECRETARY);
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

    private Member saveMember(String fullName, String userId, OrganizationUnit organization, MemberRole role) {
        Member member = new Member();
        member.setFullName(fullName);
        member.setUserId(userId);
        member.setEmail(userId + "@example.com");
        member.setMemberRole(role);
        member.setMemberStatus(MemberStatus.ACTIVE);
        member.setOrganization(organization);
        return memberRepository.save(member);
    }

    private MockHttpServletRequestBuilder withTdpSecretary(
            MockHttpServletRequestBuilder request,
            String tdpId
    ) {
        return request
                .with(auth.identity("tdp-secretary-user", "TDP_SECRETARY", ward.getId(), tdpId, null));
    }

    private MockHttpServletRequestBuilder withMemberA(MockHttpServletRequestBuilder request) {
        return request
                .with(auth.identity(memberA.getUserId(), "MEMBER", ward.getId(), tdp1.getId(), memberA.getId()));
    }

    private MockHttpServletRequestBuilder withWardSecretary(MockHttpServletRequestBuilder request) {
        return request
                .with(auth.identity("ward-secretary-user", "WARD_SECRETARY", ward.getId(), null, null));
    }

    private MockHttpServletRequestBuilder withWardDeputySecretary(MockHttpServletRequestBuilder request) {
        return request
                .with(auth.identity("ward-deputy-user", "WARD_DEPUTY_SECRETARY", ward.getId(), null, null));
    }

}
