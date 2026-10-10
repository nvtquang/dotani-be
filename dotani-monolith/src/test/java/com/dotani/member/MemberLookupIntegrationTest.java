package com.dotani.member;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dotani.support.IntegrationTest;
import com.dotani.support.TestData.Fixture;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class MemberLookupIntegrationTest extends IntegrationTest {

    private Fixture f;

    @BeforeEach
    void setUp() {
        f = data.standardFixture();
    }

    @Test
    void anyMemberCanLookUpPublicFieldsAcrossTdps() throws Exception {
        String body = json(Map.of("memberIds", List.of(f.tdp1Secretary().getId(), f.tdp2Member().getId(), "missing-id")));

        mockMvc.perform(auth.as(f.tdp1Member(), post("/api/members/lookup"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.id == '%s')].fullName".formatted(f.tdp2Member().getId())).value("Doan vien TDP 2"))
                .andExpect(jsonPath("$[?(@.id == '%s')].organizationName".formatted(f.tdp2Member().getId())).value("Chi doan TDP 2"))
                // only directory fields: no contact, banking or role data
                .andExpect(jsonPath("$[0].email").doesNotExist())
                .andExpect(jsonPath("$[0].phone").doesNotExist())
                .andExpect(jsonPath("$[0].accountNumber").doesNotExist())
                .andExpect(jsonPath("$[0].memberRole").doesNotExist());
    }

    @Test
    void lookupRequiresAuthenticationAndValidatesInput() throws Exception {
        mockMvc.perform(post("/api/members/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("memberIds", List.of("x")))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(auth.as(f.tdp1Member(), post("/api/members/lookup"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("memberIds", List.of()))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(auth.as(f.tdp1Member(), post("/api/members/lookup"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("memberIds", Collections.nCopies(201, "id")))))
                .andExpect(status().isBadRequest());
    }
}
