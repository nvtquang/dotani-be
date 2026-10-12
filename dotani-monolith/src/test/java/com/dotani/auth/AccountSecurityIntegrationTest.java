package com.dotani.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dotani.auth.entity.UserAccount;
import com.dotani.auth.repository.UserAccountRepository;
import com.dotani.support.IntegrationTest;
import com.dotani.support.TestData.Fixture;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

class AccountSecurityIntegrationTest extends IntegrationTest {

    private static final String PASSWORD = "Password123";

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UserAccount account;

    @BeforeEach
    void setUp() {
        Fixture f = data.standardFixture();
        account = userAccountRepository.findById(f.tdp1Member().getUserId()).orElseThrow();
        account.setPasswordHash(passwordEncoder.encode(PASSWORD));
        account = userAccountRepository.save(account);
    }

    @Test
    void logoutRevokesTheRefreshToken() throws Exception {
        String refreshToken = loginOk(PASSWORD).get("refreshToken").asText();

        mockMvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isNoContent());

        refresh(refreshToken).andExpect(status().isUnauthorized());
        // logging out twice, or with garbage, is harmless
        mockMvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", "not-a-token"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void logoutFromAllDevicesRevokesEverySession() throws Exception {
        String phone = loginOk(PASSWORD).get("refreshToken").asText();
        String laptop = loginOk(PASSWORD).get("refreshToken").asText();

        mockMvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", laptop, "allDevices", true))))
                .andExpect(status().isNoContent());

        refresh(phone).andExpect(status().isUnauthorized());
    }

    @Test
    void changePasswordRequiresTheCurrentOneAndSignsOutOtherSessions() throws Exception {
        JsonNode session = loginOk(PASSWORD);
        String accessToken = session.get("accessToken").asText();
        String otherSession = loginOk(PASSWORD).get("refreshToken").asText();

        changePassword(accessToken, "wrong-password", "NewPassword456")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"));
        changePassword(accessToken, PASSWORD, "short")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WEAK_PASSWORD"));

        changePassword(accessToken, PASSWORD, "NewPassword456")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        refresh(otherSession).andExpect(status().isUnauthorized());
        login(PASSWORD).andExpect(status().isUnauthorized());
        login("NewPassword456").andExpect(status().isOk());
    }

    @Test
    void forgotPasswordEmailsASingleUseLink() throws Exception {
        mockMvc.perform(post("/api/auth/password/forgot").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "nobody@dotani.test"))))
                .andExpect(status().isNoContent());
        assertThat(mailOutbox.outbox()).isEmpty();

        mockMvc.perform(post("/api/auth/password/forgot").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", account.getEmail().toUpperCase()))))
                .andExpect(status().isNoContent());
        assertThat(mailOutbox.outbox()).singleElement().satisfies(mail -> {
            assertThat(mail.to()).isEqualTo(account.getEmail());
            assertThat(mail.link()).startsWith("http://localhost:5173/reset-password?token=");
        });
        String token = mailOutbox.outbox().getFirst().link().substring(mailOutbox.outbox().getFirst().link().indexOf("token=") + 6);

        resetPassword(token, "ResetPassword789").andExpect(status().isNoContent());
        login("ResetPassword789").andExpect(status().isOk());
        login(PASSWORD).andExpect(status().isUnauthorized());

        resetPassword(token, "AnotherPassword1")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RESET_TOKEN"));
    }

    @Test
    void fiveFailedLoginsLockTheAccount() throws Exception {
        for (int i = 0; i < 5; i++) {
            login("wrong-password").andExpect(status().isUnauthorized());
        }

        // even the right password is refused while locked
        login(PASSWORD)
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
        assertThat(userAccountRepository.findById(account.getId()).orElseThrow().getLockedUntil()).isNotNull();
    }

    @Test
    void successfulLoginResetsTheFailureCounter() throws Exception {
        for (int i = 0; i < 4; i++) {
            login("wrong-password").andExpect(status().isUnauthorized());
        }
        login(PASSWORD).andExpect(status().isOk());
        login("wrong-password").andExpect(status().isUnauthorized());
        login(PASSWORD).andExpect(status().isOk());
    }

    @Test
    void loginIsRateLimitedPerClientIp() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/auth/login").header("X-Forwarded-For", "198.51.100.20")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("usernameOrEmail", "nobody", "password", "x"))))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login").header("X-Forwarded-For", "198.51.100.20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("usernameOrEmail", "nobody", "password", "x"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));

        // another client is not affected
        login(PASSWORD).andExpect(status().isOk());
    }

    private ResultActions login(String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("usernameOrEmail", account.getUsername(), "password", password))));
    }

    private JsonNode loginOk(String password) throws Exception {
        return body(login(password).andExpect(status().isOk()).andReturn());
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("refreshToken", refreshToken))));
    }

    private ResultActions changePassword(String accessToken, String current, String next) throws Exception {
        return mockMvc.perform(put("/api/auth/password")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("currentPassword", current, "newPassword", next))));
    }

    private ResultActions resetPassword(String token, String newPassword) throws Exception {
        return mockMvc.perform(post("/api/auth/password/reset").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("token", token, "newPassword", newPassword))));
    }
}
