package com.dotani.shared;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dotani.auth.repository.UserAccountRepository;
import com.dotani.support.IntegrationTest;
import com.dotani.support.TestData.Fixture;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/** Errors keep a stable code and carry a Vietnamese message the frontend can show as is. */
class ErrorMessagesIntegrationTest extends IntegrationTest {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Test
    void businessErrorsAreTranslated() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("usernameOrEmail", "nobody", "password", "x"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Tên đăng nhập/email hoặc mật khẩu không đúng."));
    }

    @Test
    void missingAuthenticationIsTranslated() throws Exception {
        mockMvc.perform(get("/api/members/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Bạn cần đăng nhập để tiếp tục."));
    }

    @Test
    void validationDetailsAreTranslated() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("usernameOrEmail", "", "password", "x"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Dữ liệu chưa hợp lệ. Vui lòng kiểm tra lại các trường."))
                .andExpect(jsonPath("$.details").value(hasItem("usernameOrEmail: không được để trống")));
    }

    @Test
    void messagesFillInArguments() throws Exception {
        Fixture f = data.standardFixture();
        var account = userAccountRepository.findById(f.tdp1Member().getUserId()).orElseThrow();
        account.setLockedUntil(Instant.now().plus(10, ChronoUnit.MINUTES));
        userAccountRepository.save(account);

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("usernameOrEmail", account.getUsername(), "password", "x"))))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.message").value(
                        "Tài khoản tạm khoá do đăng nhập sai quá nhiều lần. Vui lòng thử lại sau 10 phút."));
    }
}
