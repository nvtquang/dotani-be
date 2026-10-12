package com.dotani.shared;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dotani.support.IntegrationTest;
import com.dotani.support.TestData.Fixture;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Responses carry display names, and list endpoints share the page/size contract. */
class ResponseContractIntegrationTest extends IntegrationTest {

    @Test
    void postsEventsAndParticipantsCarryNames() throws Exception {
        Fixture f = data.standardFixture();

        var post = mockMvc.perform(auth.as(f.wardSecretary(), post("/api/posts"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "Bai", "content", "Noi dung", "type", "NEWS",
                                "organizationId", f.tdp1().getId(), "status", "PUBLISHED"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorName").value("Bi thu phuong"))
                .andExpect(jsonPath("$.organizationName").value("Chi doan TDP 1"))
                .andReturn();

        mockMvc.perform(auth.as(f.tdp1Member(), get("/api/posts")))
                .andExpect(jsonPath("$.content[0].authorName").value("Bi thu phuong"));

        var event = mockMvc.perform(auth.as(f.tdp1Secretary(), post("/api/events"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "Sinh hoat", "type", "MEETING",
                                "startTime", "2026-12-01T01:00:00Z", "endTime", "2026-12-01T03:00:00Z",
                                "organizationId", f.tdp1().getId(), "status", "PUBLISHED"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.organizationName").value("Chi doan TDP 1"))
                .andReturn();
        String eventId = body(event).get("id").asText();

        mockMvc.perform(auth.as(f.tdp1Member(), put("/api/events/{id}/participation", eventId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "GOING"))))
                .andExpect(status().isOk());
        mockMvc.perform(auth.as(f.tdp1Secretary(), get("/api/events/{id}/participants", eventId)))
                .andExpect(jsonPath("$.content[0].participantName").value("Doan vien TDP 1"));
    }

    @Test
    void listEndpointsAcceptPageAndSize() throws Exception {
        Fixture f = data.standardFixture();

        mockMvc.perform(auth.as(f.wardSecretary(), get("/api/organizations").param("page", "0").param("size", "2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
        mockMvc.perform(auth.as(f.wardSecretary(), get("/api/organizations").param("page", "1").param("size", "2")))
                .andExpect(jsonPath("$.content.length()").value(1));
        mockMvc.perform(auth.as(f.tdp1Member(), get("/api/chat/conversations").param("size", "5")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
