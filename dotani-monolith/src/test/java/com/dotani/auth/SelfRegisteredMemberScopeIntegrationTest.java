package com.dotani.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dotani.member.entity.MemberRole;
import com.dotani.member.entity.OrganizationUnit;
import com.dotani.support.IntegrationTest;
import com.dotani.shared.time.AppTime;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** A self-registered account must get the same scope shape as seeded ones (ward + TDP). */
class SelfRegisteredMemberScopeIntegrationTest extends IntegrationTest {

    @Test
    void selfRegisteredMemberCanSeeWardLevelEvents() throws Exception {
        OrganizationUnit ward = data.ward();
        OrganizationUnit tdp1 = data.tdp(ward, 1);
        var wardSecretary = data.member("Bi thu phuong", ward, MemberRole.WARD_SECRETARY);

        var registration = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "new.member",
                                "fullName", "Doan vien moi",
                                "email", "new.member@dotani.test",
                                "password", "Password@123",
                                "organizationId", tdp1.getId()
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.organizationId").value(ward.getId()))
                .andExpect(jsonPath("$.user.tdpId").value(tdp1.getId()))
                .andReturn();
        String token = body(registration).get("accessToken").asText();

        Map<String, Object> event = new LinkedHashMap<>();
        event.put("title", "Dai hoi doan phuong");
        event.put("type", "CONGRESS");
        event.put("startTime", OffsetDateTime.now(AppTime.ZONE).plusDays(3).withNano(0).toInstant().toString());
        event.put("endTime", OffsetDateTime.now(AppTime.ZONE).plusDays(3).plusHours(3).withNano(0).toInstant().toString());
        event.put("organizationId", ward.getId());
        event.put("status", "PUBLISHED");
        var created = mockMvc.perform(auth.as(wardSecretary, post("/api/events"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(event)))
                .andExpect(status().isCreated())
                .andReturn();
        String eventId = body(created).get("id").asText();

        mockMvc.perform(get("/api/events/{id}", eventId).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Dai hoi doan phuong"));
    }
}
