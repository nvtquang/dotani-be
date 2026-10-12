package com.dotani.member;

import com.dotani.support.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dotani.member.entity.OrganizationUnit;
import com.dotani.member.entity.OrganizationUnitType;
import com.dotani.member.repository.MemberRepository;
import com.dotani.member.repository.OrganizationUnitRepository;
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

class OrganizationIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationUnitRepository organizationUnitRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private com.dotani.member.service.MemberRegistrationService memberRegistrationService;

    private OrganizationUnit ward;
    private OrganizationUnit tdp1;
    private OrganizationUnit tdp2;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
        organizationUnitRepository.deleteAll();

        ward = new OrganizationUnit();
        ward.setName("Phuong Thuong Cat");
        ward.setCode("THUONG_CAT");
        ward.setType(OrganizationUnitType.WARD);
        ward = organizationUnitRepository.save(ward);

        tdp1 = new OrganizationUnit();
        tdp1.setName("Chi doan TDP 1");
        tdp1.setCode("TDP_1");
        tdp1.setType(OrganizationUnitType.YOUTH_UNION_BRANCH);
        tdp1.setParent(ward);
        tdp1 = organizationUnitRepository.save(tdp1);

        tdp2 = new OrganizationUnit();
        tdp2.setName("Chi doan TDP 2");
        tdp2.setCode("TDP_2");
        tdp2.setType(OrganizationUnitType.YOUTH_UNION_BRANCH);
        tdp2.setParent(ward);
        tdp2 = organizationUnitRepository.save(tdp2);
    }

    @Test
    void wardSecretaryCanReadAllOrganizations() throws Exception {
        mockMvc.perform(withWardSecretary(get("/api/organizations")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3));
    }

    @Test
    void publicOrganizationListReturnsActiveTdpBranchesForRegistration() throws Exception {
        mockMvc.perform(get("/api/organizations/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[?(@.id == '%s')]".formatted(tdp1.getId())).exists())
                .andExpect(jsonPath("$.content[?(@.id == '%s')]".formatted(tdp2.getId())).exists())
                .andExpect(jsonPath("$.content[?(@.id == '%s')]".formatted(ward.getId())).doesNotExist());
    }

    @Test
    void registrationCreatesMemberProfileInSelectedTdpAndReportsItsWard() {
        // in the real flow the login account is created first
        data.ensureAccount("registered-user-1");
        var registered = memberRegistrationService.register(
                "registered-user-1",
                "Nguyen Van Dang Ky",
                "0900000000",
                null,
                tdp1.getId()
        );

        assertThat(registered.tdpId()).isEqualTo(tdp1.getId());
        assertThat(registered.wardId()).isEqualTo(ward.getId());
        var member = memberRepository.findById(registered.memberId()).orElseThrow();
        assertThat(member.getUserId()).isEqualTo("registered-user-1");
        assertThat(member.getOrganization().getId()).isEqualTo(tdp1.getId());
        assertThat(member.getMemberRole().name()).isEqualTo("MEMBER");
    }

    @Test
    void tdpSecretaryCanOnlySeeWardAndOwnTdpInList() throws Exception {
        MvcResult result = mockMvc.perform(withTdpSecretary(get("/api/organizations"), tdp1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(response.findValuesAsText("id")).contains(ward.getId(), tdp1.getId());
        assertThat(response.findValuesAsText("id")).doesNotContain(tdp2.getId());
    }

    @Test
    void tdpSecretaryOfTdp1CannotReadTdp2() throws Exception {
        mockMvc.perform(withTdpSecretary(get("/api/organizations/{id}", tdp2.getId()), tdp1.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));
    }

    @Test
    void tdpSecretaryOfTdp1CannotUpdateTdp2() throws Exception {
        mockMvc.perform(withTdpSecretary(put("/api/organizations/{id}", tdp2.getId()), tdp1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Chi doan TDP 2 updated",
                                "code", "TDP_2_UPDATED",
                                "type", "YOUTH_UNION_BRANCH",
                                "parentId", ward.getId()
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));

        OrganizationUnit unchanged = organizationUnitRepository.findById(tdp2.getId()).orElseThrow();
        assertThat(unchanged.getCode()).isEqualTo("TDP_2");
    }

    @Test
    void tdpSecretaryOfTdp1CannotDeleteTdp2() throws Exception {
        mockMvc.perform(withTdpSecretary(delete("/api/organizations/{id}", tdp2.getId()), tdp1.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));

        assertThat(organizationUnitRepository.existsById(tdp2.getId())).isTrue();
    }

    @Test
    void tdpSecretaryCanUpdateOwnTdp() throws Exception {
        mockMvc.perform(withTdpSecretary(put("/api/organizations/{id}", tdp1.getId()), tdp1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Chi doan TDP 1 moi",
                                "code", "TDP_1_NEW",
                                "type", "YOUTH_UNION_BRANCH",
                                "parentId", ward.getId()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tdp1.getId()))
                .andExpect(jsonPath("$.code").value("TDP_1_NEW"));
    }

    @Test
    void memberCannotUpdateOrganization() throws Exception {
        mockMvc.perform(withMember(put("/api/organizations/{id}", tdp1.getId()), tdp1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Chi doan TDP 1 moi",
                                "code", "TDP_1_NEW",
                                "type", "YOUTH_UNION_BRANCH",
                                "parentId", ward.getId()
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));
    }

    @Test
    void wardSecretaryCanCreateOrganization() throws Exception {
        mockMvc.perform(withWardSecretary(post("/api/organizations"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Chi doan TDP 3",
                                "code", "TDP_3",
                                "type", "YOUTH_UNION_BRANCH",
                                "parentId", ward.getId()
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Chi doan TDP 3"))
                .andExpect(jsonPath("$.type").value("YOUTH_UNION_BRANCH"));
    }

    @Test
    void tdpSecretaryCannotCreateOrganization() throws Exception {
        mockMvc.perform(withTdpSecretary(post("/api/organizations"), tdp1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "name", "Chi doan TDP 3",
                                "code", "TDP_3",
                                "type", "YOUTH_UNION_BRANCH",
                                "parentId", ward.getId()
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void getOrganizationMembersChecksScopeBeforeReturningEmptyList() throws Exception {
        mockMvc.perform(withTdpSecretary(get("/api/organizations/{id}/members", tdp2.getId()), tdp1.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));

        mockMvc.perform(withTdpSecretary(get("/api/organizations/{id}/members", tdp1.getId()), tdp1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void organizationApiWithoutUserContextReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/organizations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
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

    private MockHttpServletRequestBuilder withMember(MockHttpServletRequestBuilder request, String tdpId) {
        return request
                .with(auth.identity("member-user", "MEMBER", ward.getId(), tdpId, "member-1"));
    }

}
