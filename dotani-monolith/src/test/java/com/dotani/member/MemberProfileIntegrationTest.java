package com.dotani.member;

import com.dotani.support.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class MemberProfileIntegrationTest extends IntegrationTest {

    private static final Path AVATAR_STORAGE_PATH = Path.of("target/test-storage/avatars");

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
    private Member currentMember;
    private Member otherMember;

    @BeforeEach
    void setUp() throws Exception {
        cleanStorage();
        auditRepository.deleteAll();
        memberRepository.deleteAll();
        organizationUnitRepository.deleteAll();

        ward = saveOrganization("Phuong Thuong Cat", "THUONG_CAT", OrganizationUnitType.WARD, null);
        tdp1 = saveOrganization("Chi doan TDP 1", "TDP_1", OrganizationUnitType.YOUTH_UNION_BRANCH, ward);
        tdp2 = saveOrganization("Chi doan TDP 2", "TDP_2", OrganizationUnitType.YOUTH_UNION_BRANCH, ward);

        currentMember = saveMember("Nguyen Van An", "current-user", tdp1);
        otherMember = saveMember("Tran Thi Binh", "other-user", tdp2);
    }

    @Test
    void memberCanReadAndUpdateOwnProfileWithoutChangingAdministrativeFields() throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("fullName", "Nguyen Van An Updated");
        payload.put("dateOfBirth", "2004-04-15");
        payload.put("gender", "MALE");
        payload.put("phone", "0912345678");
        payload.put("email", "updated@example.com");
        payload.put("address", "Thuong Cat updated");
        payload.put("memberRole", "WARD_SECRETARY");
        payload.put("organizationId", tdp2.getId());
        payload.put("memberStatus", "INACTIVE");
        payload.put("youthUnionJoinDate", "2026-03-26");

        mockMvc.perform(withMember(put("/api/members/me"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(currentMember.getId()))
                .andExpect(jsonPath("$.fullName").value("Nguyen Van An Updated"))
                .andExpect(jsonPath("$.email").value("updated@example.com"))
                .andExpect(jsonPath("$.organizationId").value(tdp1.getId()))
                .andExpect(jsonPath("$.memberStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.memberRole").value("MEMBER"))
                .andExpect(jsonPath("$.youthUnionJoinDate").value("2022-03-26"));

        Member updated = memberRepository.findById(currentMember.getId()).orElseThrow();
        assertThat(updated.getOrganization().getId()).isEqualTo(tdp1.getId());
        assertThat(updated.getMemberStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(updated.getMemberRole()).isEqualTo(MemberRole.MEMBER);
        assertThat(updated.getYouthUnionJoinDate()).isEqualTo(LocalDate.of(2022, 3, 26));

        mockMvc.perform(withMember(get("/api/members/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Nguyen Van An Updated"));
    }

    @Test
    void allRolesCanUpdateOwnPersonalProfile() throws Exception {
        Map<MemberRole, OrganizationUnit> organizationsByRole = Map.of(
                MemberRole.WARD_SECRETARY, tdp1,
                MemberRole.WARD_DEPUTY_SECRETARY, tdp1,
                MemberRole.TDP_SECRETARY, tdp1,
                MemberRole.TDP_DEPUTY_SECRETARY, tdp1,
                MemberRole.MEMBER, tdp1
        );

        for (MemberRole role : MemberRole.values()) {
            Member roleMember = saveMember("Profile " + role.name(), "profile-" + role.name(), organizationsByRole.get(role), role);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("fullName", "Updated " + role.name());
            payload.put("dateOfBirth", "2004-04-15");
            payload.put("gender", "MALE");
            payload.put("phone", "0912345678");
            payload.put("email", role.name().toLowerCase() + "@example.com");
            payload.put("address", "Thuong Cat updated");

            mockMvc.perform(withUser(put("/api/members/me"), roleMember, role)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(payload)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(roleMember.getId()))
                    .andExpect(jsonPath("$.fullName").value("Updated " + role.name()))
                    .andExpect(jsonPath("$.memberRole").value(role.name()));
        }
    }

    @Test
    void memberCanUploadAndDeleteAvatar() throws Exception {
        MockMultipartFile avatar = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[] {1, 2, 3, 4}
        );

        mockMvc.perform(withMember(multipart("/api/members/me/avatar").file(avatar)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value(org.hamcrest.Matchers.startsWith("/uploads/avatars/")));

        Member withAvatar = memberRepository.findById(currentMember.getId()).orElseThrow();
        assertThat(withAvatar.getAvatarUrl()).startsWith("/uploads/avatars/");
        String storedFilename = withAvatar.getAvatarUrl().substring("/uploads/avatars/".length());
        assertThat(storedFilename).doesNotContain("avatar.png");
        assertThat(Files.exists(AVATAR_STORAGE_PATH.resolve(storedFilename))).isTrue();

        mockMvc.perform(get(withAvatar.getAvatarUrl()))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[] {1, 2, 3, 4}));

        mockMvc.perform(withMember(delete("/api/members/me/avatar")))
                .andExpect(status().isNoContent());

        Member withoutAvatar = memberRepository.findById(currentMember.getId()).orElseThrow();
        assertThat(withoutAvatar.getAvatarUrl()).isNull();
        assertThat(Files.exists(AVATAR_STORAGE_PATH.resolve(storedFilename))).isFalse();
    }

    @Test
    void uploadAvatarRejectsInvalidFileType() throws Exception {
        MockMultipartFile avatar = new MockMultipartFile(
                "file",
                "avatar.txt",
                "text/plain",
                new byte[] {1, 2, 3}
        );

        mockMvc.perform(withMember(multipart("/api/members/me/avatar").file(avatar)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_AVATAR_FILE"));
    }

    @Test
    void uploadAvatarRejectsTooLargeFile() throws Exception {
        MockMultipartFile avatar = new MockMultipartFile(
                "file",
                "avatar.webp",
                "image/webp",
                new byte[17]
        );

        mockMvc.perform(withMember(multipart("/api/members/me/avatar").file(avatar)))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("AVATAR_TOO_LARGE"));
    }

    @Test
    void memberCannotUpdateAnotherMemberProfileThroughManagementEndpoint() throws Exception {
        mockMvc.perform(withMember(put("/api/members/{id}", otherMember.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "userId", "other-user",
                                "fullName", "Blocked Update",
                                "dateOfBirth", "2004-04-15",
                                "gender", "FEMALE",
                                "phone", "0900000000",
                                "email", "other@example.com",
                                "address", "TDP 2",
                                "youthUnionJoinDate", "2022-03-26",
                                "memberStatus", "ACTIVE",
                                "organizationId", tdp2.getId()
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));

        assertThat(memberRepository.findById(otherMember.getId()).orElseThrow().getFullName())
                .isEqualTo("Tran Thi Binh");
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

    private Member saveMember(String fullName, String userId, OrganizationUnit organization) {
        return saveMember(fullName, userId, organization, MemberRole.MEMBER);
    }

    private Member saveMember(String fullName, String userId, OrganizationUnit organization, MemberRole role) {
        Member member = new Member();
        member.setFullName(fullName);
        member.setUserId(userId);
        member.setEmail(userId + "@example.com");
        member.setMemberStatus(MemberStatus.ACTIVE);
        member.setMemberRole(role);
        member.setYouthUnionJoinDate(LocalDate.of(2022, 3, 26));
        member.setOrganization(organization);
        return memberRepository.save(member);
    }

    private MockHttpServletRequestBuilder withMember(MockHttpServletRequestBuilder request) {
        return request
                .with(auth.identity(currentMember.getUserId(), "MEMBER", ward.getId(), tdp1.getId(), currentMember.getId()));
    }

    private MockHttpServletRequestBuilder withUser(
            MockHttpServletRequestBuilder request,
            Member member,
            MemberRole role
    ) {
        return request
                .with(auth.identity(member.getUserId(), role.name(), ward.getId(), role.isWardOfficer() ? "" : member.getOrganization().getId(), member.getId()));
    }


    private void cleanStorage() throws Exception {
        if (!Files.exists(AVATAR_STORAGE_PATH)) {
            return;
        }
        try (var files = Files.walk(AVATAR_STORAGE_PATH)) {
            files.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    });
        }
    }
}
