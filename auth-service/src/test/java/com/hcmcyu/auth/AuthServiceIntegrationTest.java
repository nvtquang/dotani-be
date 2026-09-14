package com.hcmcyu.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hcmcyu.auth.entity.Role;
import com.hcmcyu.auth.entity.UserAccount;
import com.hcmcyu.auth.repository.RefreshTokenRepository;
import com.hcmcyu.auth.repository.UserAccountRepository;
import com.hcmcyu.auth.dto.GoogleUserInfo;
import com.hcmcyu.auth.dto.GoogleLoginRequest;
import com.hcmcyu.auth.dto.RegisterRequest;
import com.hcmcyu.auth.service.JwtService;
import com.hcmcyu.auth.service.MemberRegistrationClient;
import com.hcmcyu.auth.dto.MemberRegistrationResponse;
import com.hcmcyu.auth.service.GoogleTokenVerifier;
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

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:hcmcyu_auth_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "auth.jwt.secret=test-hcmcyu-auth-service-secret-key-change-me-please-32",
        "auth.jwt.access-token-expiration-ms=900000",
        "auth.jwt.refresh-token-expiration-ms=604800000"
})
class AuthServiceIntegrationTest {

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

    @MockBean
    private MemberRegistrationClient memberRegistrationClient;

    @MockBean
    private GoogleTokenVerifier googleTokenVerifier;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        userAccountRepository.deleteAll();
        when(memberRegistrationClient.createMemberProfile(any(UserAccount.class), any(RegisterRequest.class)))
                .thenReturn(new MemberRegistrationResponse(
                        "member-registered",
                        "user-registered",
                        "Registered Member",
                        "member01@example.com",
                        "tdp-1",
                        "Chi đoàn TDP 1"
                ));
        when(memberRegistrationClient.createMemberProfile(any(UserAccount.class), any(GoogleLoginRequest.class)))
                .thenReturn(new MemberRegistrationResponse(
                        "member-registered",
                        "user-registered",
                        "Registered Member",
                        "member01@example.com",
                        "tdp-1",
                        "Chi đoàn TDP 1"
                ));
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
        assertThat(user.getRole()).isEqualTo(Role.MEMBER);
        assertThat(user.getMemberId()).isEqualTo("member-registered");
        assertThat(user.getOrganizationId()).isEqualTo("tdp-1");
        assertThat(user.getTdpId()).isEqualTo("tdp-1");
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
        assertThat(user.getRole()).isEqualTo(Role.MEMBER);
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
                .andExpect(jsonPath("$.user.memberId").value("member-registered"))
                .andExpect(jsonPath("$.user.tdpId").value("tdp-1"));

        UserAccount user = userAccountRepository.findByEmail("google.member@example.com").orElseThrow();
        assertThat(user.getRole()).isEqualTo(Role.MEMBER);
        assertThat(user.getProviderSubject()).isEqualTo("google-sub-1");
        assertThat(user.getMemberId()).isEqualTo("member-registered");
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
                .andExpect(jsonPath("$.memberId").value("member-registered"))
                .andExpect(jsonPath("$.tdpId").value("tdp-1"));
    }

    @Test
    void expiredJwtReturnsUnauthorized() throws Exception {
        UserAccount user = createUser("member01", "member01@example.com", "Password123!");
        Instant now = Instant.now();
        String expiredToken = jwtService.createAccessToken(user, now.minusSeconds(120), now.minusSeconds(60));

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
        user.setRole(Role.MEMBER);
        user.setEnabled(true);
        return userAccountRepository.save(user);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
