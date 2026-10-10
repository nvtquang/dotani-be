package com.dotani;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dotani.support.IntegrationTest;
import com.dotani.support.TestData.Fixture;
import org.junit.jupiter.api.Test;

class SmokeIntegrationTest extends IntegrationTest {

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    void authenticatedMemberCanReadOwnProfile() throws Exception {
        Fixture f = data.standardFixture();

        mockMvc.perform(auth.as(f.tdp1Member(), get("/api/members/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(f.tdp1Member().getId()));
    }
}
