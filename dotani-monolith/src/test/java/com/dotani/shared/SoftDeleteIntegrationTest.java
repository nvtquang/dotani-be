package com.dotani.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dotani.auth.repository.UserAccountRepository;
import com.dotani.member.service.MemberDirectoryService;
import com.dotani.support.IntegrationTest;
import com.dotani.support.TestData.Fixture;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Deleting members, posts and events sets deleted_at: hidden everywhere, kept for history. */
class SoftDeleteIntegrationTest extends IntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MemberDirectoryService memberDirectoryService;

    @Test
    void deletedEventIsHiddenButKept() throws Exception {
        Fixture f = data.standardFixture();
        var created = mockMvc.perform(auth.as(f.wardSecretary(), post("/api/events"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "Su kien se xoa",
                                "type", "MEETING",
                                "startTime", "2026-12-01T01:00:00Z",
                                "endTime", "2026-12-01T03:00:00Z",
                                "organizationId", f.tdp1().getId(),
                                "status", "PUBLISHED"))))
                .andExpect(status().isCreated())
                .andReturn();
        String eventId = body(created).get("id").asText();

        mockMvc.perform(auth.as(f.wardSecretary(), delete("/api/events/{id}", eventId)))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(auth.as(f.wardSecretary(), get("/api/events/{id}", eventId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(auth.as(f.wardSecretary(), get("/api/events")))
                .andExpect(jsonPath("$.content.length()").value(0));
        assertThat(jdbcTemplate.queryForObject(
                "select deleted_at is not null from events where id = ?", Boolean.class, eventId)).isTrue();
    }

    @Test
    void deletedMemberCannotSignInButStillHasANameInHistory() throws Exception {
        Fixture f = data.standardFixture();
        var account = userAccountRepository.findById(f.tdp1Member().getUserId()).orElseThrow();
        account.setPasswordHash(passwordEncoder.encode("Password@123"));
        userAccountRepository.save(account);

        mockMvc.perform(auth.as(f.wardSecretary(), delete("/api/members/{id}", f.tdp1Member().getId())))
                .andExpect(status().isNoContent());

        mockMvc.perform(auth.as(f.wardSecretary(), get("/api/members/{id}", f.tdp1Member().getId())))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("usernameOrEmail", account.getUsername(), "password", "Password@123"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_LINKED"));

        assertThat(memberDirectoryService.findDisplayNames(List.of(f.tdp1Member().getId())))
                .containsEntry(f.tdp1Member().getId(), "Doan vien TDP 1");
    }
}
