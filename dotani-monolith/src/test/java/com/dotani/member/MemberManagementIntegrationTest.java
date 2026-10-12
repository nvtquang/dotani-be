package com.dotani.member;

import com.dotani.support.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.dotani.member.repository.OrganizationUnitRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class MemberManagementIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationUnitRepository organizationUnitRepository;

    @Autowired
    private MemberRepository memberRepository;

    private OrganizationUnit ward;
    private OrganizationUnit tdp1;
    private OrganizationUnit tdp2;
    private Member memberTdp1;
    private Member memberTdp2;
    private Member wardOfficer;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
        organizationUnitRepository.deleteAll();

        ward = saveOrganization("Phuong Thuong Cat", "THUONG_CAT", OrganizationUnitType.WARD, null);
        tdp1 = saveOrganization("Chi doan TDP 1", "TDP_1", OrganizationUnitType.YOUTH_UNION_BRANCH, ward);
        tdp2 = saveOrganization("Chi doan TDP 2", "TDP_2", OrganizationUnitType.YOUTH_UNION_BRANCH, ward);

        memberTdp1 = saveMember("Nguyen Van An", "user-tdp-1", "an@example.com", tdp1, MemberRole.MEMBER);
        memberTdp2 = saveMember("Tran Thi Binh", "user-tdp-2", "binh@example.com", tdp2, MemberRole.MEMBER);
        wardOfficer = saveMember("Le Can Bo Phuong", "ward-officer-user", "ward@example.com", tdp1, MemberRole.WARD_SECRETARY);
    }

    @Test
    void wardSecretaryCanCrudMembersAcrossWard() throws Exception {
        MvcResult result = mockMvc.perform(withWardSecretary(post("/api/members"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(memberPayload("Pham Minh Chau", "new-user", tdp2.getId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("Pham Minh Chau"))
                .andExpect(jsonPath("$.memberRole").value("MEMBER"))
                .andReturn();

        String createdMemberId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(withWardSecretary(get("/api/members/{id}", createdMemberId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationId").value(tdp2.getId()));

        mockMvc.perform(withWardSecretary(put("/api/members/{id}", createdMemberId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(memberPayload("Pham Minh Chau Updated", "new-user", tdp1.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationId").value(tdp1.getId()))
                .andExpect(jsonPath("$.fullName").value("Pham Minh Chau Updated"));

        mockMvc.perform(withWardSecretary(delete("/api/members/{id}", createdMemberId)))
                .andExpect(status().isNoContent());

        // soft deleted: hidden from queries but the row is kept
        assertThat(memberRepository.findById(createdMemberId)).isEmpty();
        assertThat(memberRepository.existsAnyById(createdMemberId)).isTrue();
    }

    @Test
    void tdpSecretaryCanOnlyListMembersInOwnTdp() throws Exception {
        mockMvc.perform(withTdpSecretary(get("/api/members"), tdp1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[?(@.id == '%s')]".formatted(memberTdp1.getId())).exists())
                .andExpect(jsonPath("$.content[?(@.id == '%s')]".formatted(wardOfficer.getId())).exists())
                .andExpect(jsonPath("$.content[?(@.id == '%s')]".formatted(memberTdp2.getId())).doesNotExist());
    }

    @Test
    void tdpSecretaryCannotReadUpdateOrDeleteMemberFromAnotherTdp() throws Exception {
        mockMvc.perform(withTdpSecretary(get("/api/members/{id}", memberTdp2.getId()), tdp1.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));

        mockMvc.perform(withTdpSecretary(put("/api/members/{id}", memberTdp2.getId()), tdp1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(memberPayload("Blocked Update", "user-tdp-2", tdp2.getId()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));

        mockMvc.perform(withTdpSecretary(delete("/api/members/{id}", memberTdp2.getId()), tdp1.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));

        Member unchanged = memberRepository.findById(memberTdp2.getId()).orElseThrow();
        assertThat(unchanged.getFullName()).isEqualTo("Tran Thi Binh");
        assertThat(unchanged.getMemberStatus()).isEqualTo(MemberStatus.ACTIVE);
    }

    @Test
    void tdpSecretaryCannotCreateOrMoveMemberIntoAnotherTdp() throws Exception {
        mockMvc.perform(withTdpSecretary(post("/api/members"), tdp1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(memberPayload("Cross TDP Create", "cross-user", tdp2.getId()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));

        mockMvc.perform(withTdpSecretary(put("/api/members/{id}", memberTdp1.getId()), tdp1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(memberPayload("Cross TDP Move", "user-tdp-1", tdp2.getId()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));

        assertThat(memberRepository.findById(memberTdp1.getId()).orElseThrow().getOrganization().getId())
                .isEqualTo(tdp1.getId());
    }

    @Test
    void tdpSecretaryCannotChangeOrDeleteWardOfficerEvenInsideOwnTdp() throws Exception {
        mockMvc.perform(withTdpSecretary(put("/api/members/{id}", wardOfficer.getId()), tdp1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(memberPayload("Ward Officer Updated", "ward-officer-user", tdp1.getId()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));

        mockMvc.perform(withTdpSecretary(delete("/api/members/{id}", wardOfficer.getId()), tdp1.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));

        Member unchanged = memberRepository.findById(wardOfficer.getId()).orElseThrow();
        assertThat(unchanged.getFullName()).isEqualTo("Le Can Bo Phuong");
        assertThat(unchanged.getMemberRole()).isEqualTo(MemberRole.WARD_SECRETARY);
    }

    @Test
    void tdpSecretaryCanCrudMemberInOwnTdpButCannotSetRoleFromPayload() throws Exception {
        Map<String, Object> payload = memberPayload("Own TDP Create", "own-create-user", tdp1.getId());
        payload.put("memberRole", "WARD_SECRETARY");

        MvcResult result = mockMvc.perform(withTdpSecretary(post("/api/members"), tdp1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberRole").value("MEMBER"))
                .andReturn();

        String createdMemberId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(withTdpSecretary(put("/api/members/{id}", createdMemberId), tdp1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(memberPayload("Own TDP Updated", "own-create-user", tdp1.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Own TDP Updated"))
                .andExpect(jsonPath("$.memberRole").value("MEMBER"));

        mockMvc.perform(withTdpSecretary(delete("/api/members/{id}", createdMemberId), tdp1.getId()))
                .andExpect(status().isNoContent());

        // soft deleted: hidden from queries but the row is kept
        assertThat(memberRepository.findById(createdMemberId)).isEmpty();
        assertThat(memberRepository.existsAnyById(createdMemberId)).isTrue();
    }

    @Test
    void tdpDeputySecretaryCanCrudMemberInOwnTdpWithSameScopeAsTdpSecretary() throws Exception {
        MvcResult result = mockMvc.perform(withTdpDeputySecretary(post("/api/members"), tdp1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(memberPayload("Deputy Created", "deputy-created-user", tdp1.getId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberRole").value("MEMBER"))
                .andExpect(jsonPath("$.organizationId").value(tdp1.getId()))
                .andReturn();

        String createdMemberId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(withTdpDeputySecretary(get("/api/members/{id}", createdMemberId), tdp1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdMemberId));

        mockMvc.perform(withTdpDeputySecretary(put("/api/members/{id}", createdMemberId), tdp1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(memberPayload("Deputy Updated", "deputy-created-user", tdp1.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Deputy Updated"));

        mockMvc.perform(withTdpDeputySecretary(get("/api/members/{id}", memberTdp2.getId()), tdp1.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));

        mockMvc.perform(withTdpDeputySecretary(delete("/api/members/{id}", createdMemberId), tdp1.getId()))
                .andExpect(status().isNoContent());

        // soft deleted: hidden from queries but the row is kept
        assertThat(memberRepository.findById(createdMemberId)).isEmpty();
        assertThat(memberRepository.existsAnyById(createdMemberId)).isTrue();
    }

    @Test
    void memberCannotCrudOtherMembersButCanReadSelf() throws Exception {
        mockMvc.perform(withMember(get("/api/members/{id}", memberTdp1.getId()), memberTdp1.getId(), tdp1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(memberTdp1.getId()));

        mockMvc.perform(withMember(get("/api/members/{id}", memberTdp2.getId()), memberTdp1.getId(), tdp1.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));

        mockMvc.perform(withMember(post("/api/members"), memberTdp1.getId(), tdp1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(memberPayload("Member Create Blocked", "member-create", tdp1.getId()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));

        mockMvc.perform(withMember(put("/api/members/{id}", memberTdp2.getId()), memberTdp1.getId(), tdp1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(memberPayload("Member Update Blocked", "user-tdp-2", tdp2.getId()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));

        mockMvc.perform(withMember(delete("/api/members/{id}", memberTdp2.getId()), memberTdp1.getId(), tdp1.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));
    }

    @Test
    void supportsSearchFilterPaginationAndSoftDeletedStatus() throws Exception {
        memberTdp2.setMemberStatus(MemberStatus.INACTIVE);
        memberRepository.save(memberTdp2);

        mockMvc.perform(withWardSecretary(get("/api/members")
                        .param("keyword", "binh")
                        .param("status", "INACTIVE")
                        .param("organizationId", tdp2.getId())
                        .param("page", "0")
                        .param("size", "5")
                        .param("sort", "fullName,asc")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(memberTdp2.getId()))
                .andExpect(jsonPath("$.content[0].memberStatus").value("INACTIVE"));
    }

    @Test
    void supportsRoleFilter() throws Exception {
        mockMvc.perform(withWardSecretary(get("/api/members")
                        .param("role", "WARD_SECRETARY")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sort", "fullName,asc")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(wardOfficer.getId()))
                .andExpect(jsonPath("$.content[0].memberRole").value("WARD_SECRETARY"));
    }

    @Test
    void duplicateUserIdReturnsConflict() throws Exception {
        mockMvc.perform(withWardSecretary(post("/api/members"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(memberPayload("Duplicate User", "user-tdp-1", tdp1.getId()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_MEMBER_USER_ID"));
    }

    @Test
    void organizationMembersEndpointReturnsMembersWithinScope() throws Exception {
        mockMvc.perform(withTdpSecretary(get("/api/organizations/{id}/members", tdp1.getId()), tdp1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.memberId == '%s')]".formatted(memberTdp1.getId())).exists())
                .andExpect(jsonPath("$[?(@.memberId == '%s')]".formatted(memberTdp2.getId())).doesNotExist());
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

    private Member saveMember(
            String fullName,
            String userId,
            String email,
            OrganizationUnit organization,
            MemberRole role
    ) {
        Member member = new Member();
        member.setFullName(fullName);
        member.setUserId(userId == null ? null : data.ensureAccount(userId));
        member.setEmail(email);
        member.setMemberStatus(MemberStatus.ACTIVE);
        member.setMemberRole(role);
        member.setOrganization(organization);
        return memberRepository.save(member);
    }

    private Map<String, Object> memberPayload(String fullName, String userId, String organizationId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", data.ensureAccount(userId));
        payload.put("fullName", fullName);
        payload.put("dateOfBirth", "2004-04-15");
        payload.put("gender", "MALE");
        payload.put("phone", "0900000000");
        payload.put("email", userId + "@example.com");
        payload.put("address", "Thuong Cat");
        payload.put("avatarUrl", "https://example.com/avatar.png");
        payload.put("youthUnionJoinDate", "2022-03-26");
        payload.put("memberStatus", "ACTIVE");
        payload.put("organizationId", organizationId);
        return payload;
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

    private MockHttpServletRequestBuilder withTdpDeputySecretary(
            MockHttpServletRequestBuilder request,
            String tdpId
    ) {
        return request
                .with(auth.identity("tdp-deputy-secretary-user", "TDP_DEPUTY_SECRETARY", ward.getId(), tdpId, null));
    }

    private MockHttpServletRequestBuilder withMember(
            MockHttpServletRequestBuilder request,
            String memberId,
            String tdpId
    ) {
        return request
                .with(auth.identity("member-user", "MEMBER", ward.getId(), tdpId, memberId));
    }

}
