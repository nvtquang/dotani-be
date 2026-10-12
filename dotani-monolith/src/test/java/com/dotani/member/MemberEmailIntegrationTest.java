package com.dotani.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dotani.auth.repository.UserAccountRepository;
import com.dotani.member.repository.MemberRepository;
import com.dotani.support.IntegrationTest;
import com.dotani.support.TestData.Fixture;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;

/** An email is stored once: on the login account if there is one, otherwise on the member. */
class MemberEmailIntegrationTest extends IntegrationTest {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Fixture f;

    @BeforeEach
    void setUp() {
        f = data.standardFixture();
    }

    @Test
    void linkedMemberShowsAndEditsTheLoginEmail() throws Exception {
        String loginEmail = userAccountRepository.findById(f.tdp1Member().getUserId()).orElseThrow().getEmail();
        mockMvc.perform(auth.as(f.tdp1Member(), get("/api/members/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(loginEmail));

        mockMvc.perform(auth.as(f.tdp1Member(), put("/api/members/me"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fullName", "Doan vien TDP 1", "email", "New.Address@Dotani.test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new.address@dotani.test"));

        assertThat(userAccountRepository.findById(f.tdp1Member().getUserId()).orElseThrow().getEmail())
                .isEqualTo("new.address@dotani.test");
        assertThat(memberRepository.findById(f.tdp1Member().getId()).orElseThrow().getEmail()).isNull();
    }

    @Test
    void emailOfAnotherAccountIsRejected() throws Exception {
        String takenEmail = userAccountRepository.findById(f.tdp2Member().getUserId()).orElseThrow().getEmail();

        mockMvc.perform(auth.as(f.tdp1Member(), put("/api/members/me"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fullName", "Doan vien TDP 1", "email", takenEmail))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));
    }

    @Test
    void memberWithoutAccountKeepsAContactEmailAndIsFoundByIt() throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("fullName", "Doan vien chua co tai khoan");
        payload.put("email", "contact@dotani.test");
        payload.put("organizationId", f.tdp1().getId());

        var created = mockMvc.perform(auth.as(f.wardSecretary(), post("/api/members"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("contact@dotani.test"))
                .andReturn();
        String memberId = body(created).get("id").asText();
        assertThat(memberRepository.findById(memberId).orElseThrow().getEmail()).isEqualTo("contact@dotani.test");

        mockMvc.perform(auth.as(f.wardSecretary(), get("/api/members").param("keyword", "contact@dotani")))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(memberId));
    }

    @Test
    void searchFindsLinkedMembersByLoginEmail() throws Exception {
        String loginEmail = userAccountRepository.findById(f.tdp2Member().getUserId()).orElseThrow().getEmail();

        mockMvc.perform(auth.as(f.wardSecretary(), get("/api/members").param("keyword", loginEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(f.tdp2Member().getId()));
    }
}
