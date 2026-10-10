package com.dotani.member;

import com.dotani.support.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

class RoleManagementIntegrationTest extends IntegrationTest {

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
    private Member targetMember;

    @BeforeEach
    void setUp() {
        auditRepository.deleteAll();
        memberRepository.deleteAll();
        organizationUnitRepository.deleteAll();

        ward = saveOrganization("Phuong Thuong Cat", "THUONG_CAT", OrganizationUnitType.WARD, null);
        tdp1 = saveOrganization("Chi doan TDP 1", "TDP_1", OrganizationUnitType.YOUTH_UNION_BRANCH, ward);
        targetMember = saveMember("Nguyen Van An", "target-user", tdp1, MemberRole.MEMBER);
        data.account(targetMember);
    }

    @Test
    void wardSecretaryCanUpdateRoleAndSyncAuthService() throws Exception {
        mockMvc.perform(withWardSecretary(put("/api/members/{memberId}/role", targetMember.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("role", "TDP_SECRETARY"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(targetMember.getId()))
                .andExpect(jsonPath("$.memberRole").value("TDP_SECRETARY"));

        Member updated = memberRepository.findById(targetMember.getId()).orElseThrow();
        assertThat(updated.getMemberRole()).isEqualTo(MemberRole.TDP_SECRETARY);
        assertThat(auditRepository.findAll()).singleElement()
                .satisfies(audit -> {
                    assertThat(audit.getMemberId()).isEqualTo(targetMember.getId());
                    assertThat(audit.getOldRole()).isEqualTo(MemberRole.MEMBER);
                    assertThat(audit.getNewRole()).isEqualTo(MemberRole.TDP_SECRETARY);
                    assertThat(audit.getActorUserId()).isEqualTo("ward-secretary-user");
                });
        // the login account role is synced in the same transaction
        assertThat(data.accountRole("target-user")).isEqualTo(Role.TDP_SECRETARY);
    }

    @Test
    void wardDeputySecretaryCannotUpdateRole() throws Exception {
        mockMvc.perform(withWardDeputySecretary(put("/api/members/{memberId}/role", targetMember.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("role", "TDP_SECRETARY"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ROLE_UPDATE_FORBIDDEN"));

        assertThat(memberRepository.findById(targetMember.getId()).orElseThrow().getMemberRole())
                .isEqualTo(MemberRole.MEMBER);
        assertThat(data.accountRole("target-user")).isEqualTo(Role.MEMBER);
    }

    @Test
    void tdpSecretaryCannotUpdateRole() throws Exception {
        mockMvc.perform(withTdpSecretary(put("/api/members/{memberId}/role", targetMember.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("role", "TDP_SECRETARY"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ROLE_UPDATE_FORBIDDEN"));

        assertThat(memberRepository.findById(targetMember.getId()).orElseThrow().getMemberRole())
                .isEqualTo(MemberRole.MEMBER);
    }

    @Test
    void memberCannotUpdateRole() throws Exception {
        mockMvc.perform(withMember(put("/api/members/{memberId}/role", targetMember.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("role", "TDP_SECRETARY"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ROLE_UPDATE_FORBIDDEN"));

        assertThat(memberRepository.findById(targetMember.getId()).orElseThrow().getMemberRole())
                .isEqualTo(MemberRole.MEMBER);
    }

    @Test
    void cannotRemoveLastWardSecretaryRole() throws Exception {
        Member wardSecretary = saveMember("Bi thu phuong", "last-ward-secretary-user", tdp1, MemberRole.WARD_SECRETARY);

        mockMvc.perform(withWardSecretary(put("/api/members/{memberId}/role", wardSecretary.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("role", "MEMBER"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LAST_WARD_SECRETARY"));

        assertThat(memberRepository.findById(wardSecretary.getId()).orElseThrow().getMemberRole())
                .isEqualTo(MemberRole.WARD_SECRETARY);
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
        member.setMemberStatus(MemberStatus.ACTIVE);
        member.setMemberRole(role);
        member.setOrganization(organization);
        return memberRepository.save(member);
    }

    private MockHttpServletRequestBuilder withWardSecretary(MockHttpServletRequestBuilder request) {
        return request
                .with(auth.identity("ward-secretary-user", "WARD_SECRETARY", ward.getId(), null, null));
    }

    private MockHttpServletRequestBuilder withWardDeputySecretary(MockHttpServletRequestBuilder request) {
        return request
                .with(auth.identity("ward-deputy-user", "WARD_DEPUTY_SECRETARY", ward.getId(), null, null));
    }

    private MockHttpServletRequestBuilder withTdpSecretary(MockHttpServletRequestBuilder request) {
        return request
                .with(auth.identity("tdp-secretary-user", "TDP_SECRETARY", ward.getId(), tdp1.getId(), null));
    }

    private MockHttpServletRequestBuilder withMember(MockHttpServletRequestBuilder request) {
        return request
                .with(auth.identity("member-user", "MEMBER", ward.getId(), tdp1.getId(), targetMember.getId()));
    }

}
