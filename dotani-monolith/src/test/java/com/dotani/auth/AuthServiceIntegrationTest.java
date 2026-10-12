package com.dotani.auth;

import com.dotani.support.IntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dotani.shared.security.Role;
import com.dotani.auth.entity.UserAccount;
import com.dotani.auth.repository.RefreshTokenRepository;
import com.dotani.auth.repository.UserAccountRepository;
import com.dotani.auth.dto.GoogleUserInfo;
import com.dotani.auth.dto.GoogleLoginRequest;
import com.dotani.auth.dto.RegisterRequest;
import com.dotani.auth.service.JwtService;
import com.dotani.auth.service.GoogleTokenVerifier;
import com.dotani.member.entity.OrganizationUnit;
import com.dotani.member.entity.OrganizationUnitType;
import com.dotani.member.entity.Member;
import com.dotani.member.entity.MemberRole;
import com.dotani.member.entity.MemberStatus;
import com.dotani.member.repository.MemberRepository;
import com.dotani.member.repository.OrganizationUnitRepository;
import com.dotani.member.service.MemberAccessService;
import com.dotani.shared.security.AccessScope;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AuthServiceIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private OrganizationUnitRepository organizationUnitRepository;

    @Autowired
    private MemberAccessService memberAccessService;

    @MockBean
    private GoogleTokenVerifier googleTokenVerifier;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        userAccountRepository.deleteAll();
        // Registration creates a real member profile, so the TDP it joins must exist.
        OrganizationUnit ward = data.organization("Phuong Thuong Cat", "THUONG_CAT", OrganizationUnitType.WARD, null, "ward-thuong-cat");
        data.organization("Chi doan TDP 1", "TDP_1", OrganizationUnitType.YOUTH_UNION_BRANCH, ward, "tdp-1");
        when(googleTokenVerifier.verify("valid-google-token"))
                .thenReturn(new GoogleUserInfo("google-sub-1", "google.member@example.com", "Google Member"));
    }

    @Test
    void registerCreatesMember() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "member01",
                                "fullName", "Nguyen Van A",
                                "email", "member01@example.com",
                                "password", "Password123!",
                                "organizationId", "tdp-1"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("MEMBER"));

        UserAccount user = userAccountRepository.findByUsername("member01").orElseThrow();
        assertThat(scopeOf(user).role()).isEqualTo(Role.MEMBER);
        assertThat(user.getMemberId()).isEqualTo(memberRepository.findByUserId(user.getId()).orElseThrow().getId());
        // accounts are scoped to the ward, with the TDP as tdpId (same shape as seeded accounts)
        assertThat(scopeOf(user).organizationId()).isEqualTo("ward-thuong-cat");
        assertThat(scopeOf(user).tdpId()).isEqualTo("tdp-1");
    }

    @Test
    void registerIgnoresClientSuppliedRole() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "member01",
                                "fullName", "Nguyen Van A",
                                "email", "member01@example.com",
                                "password", "Password123!",
                                "organizationId", "tdp-1",
                                "role", "WARD_SECRETARY"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("MEMBER"));

        UserAccount user = userAccountRepository.findByUsername("member01").orElseThrow();
        assertThat(scopeOf(user).role()).isEqualTo(Role.MEMBER);
    }

    @Test
    void registerRejectsDuplicateEmail() throws Exception {
        register("member01", "same@example.com", "Password123!");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "member02",
                                "fullName", "Nguyen Van B",
                                "email", "same@example.com",
                                "password", "Password123!",
                                "organizationId", "tdp-1"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));
    }

    @Test
    void registerRejectsDuplicateUsername() throws Exception {
        register("member01", "member01@example.com", "Password123!");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "member01",
                                "fullName", "Nguyen Van B",
                                "email", "member02@example.com",
                                "password", "Password123!",
                                "organizationId", "tdp-1"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_USERNAME"));
    }

    @Test
    void passwordIsHashed() throws Exception {
        register("member01", "member01@example.com", "Password123!");

        UserAccount user = userAccountRepository.findByUsername("member01").orElseThrow();
        assertThat(user.getPasswordHash()).isNotEqualTo("Password123!");
        assertThat(passwordEncoder.matches("Password123!", user.getPasswordHash())).isTrue();
    }

    @Test
    void loginWithCorrectPasswordReturnsTokens() throws Exception {
        register("member01", "member01@example.com", "Password123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "usernameOrEmail", "member01",
                                "password", "Password123!"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value("member01"));
    }

    @Test
    void loginWithWrongPasswordReturnsUnauthorized() throws Exception {
        register("member01", "member01@example.com", "Password123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "usernameOrEmail", "member01",
                                "password", "WrongPassword123!"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void googleLoginCreatesMemberAccountAndProfile() throws Exception {
        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "idToken", "valid-google-token",
                                "fullName", "Nguyễn Văn Google",
                                "phone", "0912345678",
                                "dateOfBirth", "2003-05-20",
                                "organizationId", "tdp-1",
                                "role", "WARD_SECRETARY"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("google.member@example.com"))
                .andExpect(jsonPath("$.user.role").value("MEMBER"))
                .andExpect(jsonPath("$.user.memberId").isNotEmpty())
                .andExpect(jsonPath("$.user.tdpId").value("tdp-1"));

        UserAccount user = userAccountRepository.findByEmail("google.member@example.com").orElseThrow();
        assertThat(scopeOf(user).role()).isEqualTo(Role.MEMBER);
        assertThat(user.getProviderSubject()).isEqualTo("google-sub-1");
        assertThat(user.getMemberId()).isEqualTo(memberRepository.findByUserId(user.getId()).orElseThrow().getId());
    }

    @Test
    void validJwtCanAccessProtectedMeEndpoint() throws Exception {
        String accessToken = register("member01", "member01@example.com", "Password123!").get("accessToken").asText();

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("member01"))
                .andExpect(jsonPath("$.email").value("member01@example.com"))
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.memberId").isNotEmpty())
                .andExpect(jsonPath("$.tdpId").value("tdp-1"));
    }

    @Test
    void expiredJwtReturnsUnauthorized() throws Exception {
        UserAccount user = createUser("member01", "member01@example.com", "Password123!");
        Instant now = Instant.now();
        String expiredToken = jwtService.createAccessToken(user, scopeOf(user), now.minusSeconds(120), now.minusSeconds(60));

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
    }

    @Test
    void invalidJwtReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    void protectedEndpointWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void refreshRotatesRefreshToken() throws Exception {
        String refreshToken = register("member01", "member01@example.com", "Password123!").get("refreshToken").asText();

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    private JsonNode register(String username, String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", username,
                                "fullName", "Nguyen Van A",
                                "email", email,
                                "password", password,
                                "organizationId", "tdp-1"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private UserAccount createUser(String username, String email, String password) {
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setEnabled(true);
        UserAccount saved = userAccountRepository.save(user);
        // every account is linked to a member profile, which holds its role and scope
        Member member = new Member();
        member.setFullName(username);
        member.setUserId(saved.getId());
        member.setMemberRole(MemberRole.MEMBER);
        member.setMemberStatus(MemberStatus.ACTIVE);
        member.setOrganization(organizationUnitRepository.findById("tdp-1").orElseThrow());
        saved.setMemberId(memberRepository.save(member).getId());
        return userAccountRepository.save(saved);
    }

    private AccessScope scopeOf(UserAccount user) {
        return memberAccessService.scopeOf(userAccountRepository.findById(user.getId()).orElseThrow().getMemberId());
    }

}
