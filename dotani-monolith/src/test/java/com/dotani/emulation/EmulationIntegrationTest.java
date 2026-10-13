package com.dotani.emulation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dotani.member.entity.Member;
import com.dotani.shared.time.AppTime;
import com.dotani.support.IntegrationTest;
import com.dotani.support.TestData.Fixture;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Hồ sơ thi đua: awards and yearly ratings, maintained by officers in scope, readable by the member. */
class EmulationIntegrationTest extends IntegrationTest {

    private Fixture f;
    private int year;

    @BeforeEach
    void setUp() {
        f = data.standardFixture();
        year = AppTime.today().getYear();
    }

    @Test
    void officerRecordsAwardAndMemberReadsOwnRecord() throws Exception {
        var created = mockMvc.perform(auth.as(f.tdp1Secretary(), post("/api/emulation/awards"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(award(f.tdp1Member(), "CERTIFICATE_OF_MERIT"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.kind").value("COMMENDATION"))
                .andExpect(jsonPath("$.createdByName").value("Bi thu TDP 1"))
                .andReturn();
        String awardId = body(created).get("id").asText();

        mockMvc.perform(auth.as(f.tdp1Member(), get("/api/emulation/members/{id}", f.tdp1Member().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.awards.length()").value(1))
                .andExpect(jsonPath("$.awards[0].id").value(awardId))
                .andExpect(jsonPath("$.canManage").value(false));
        mockMvc.perform(auth.as(f.tdp2Member(), get("/api/emulation/members/{id}", f.tdp1Member().getId())))
                .andExpect(status().isForbidden());

        mockMvc.perform(auth.as(f.tdp1Secretary(), delete("/api/emulation/awards/{id}", awardId)))
                .andExpect(status().isNoContent());
    }

    @Test
    void tdpOfficersCannotTouchWardOfficersOrOtherTdps() throws Exception {
        mockMvc.perform(auth.as(f.tdp1Secretary(), post("/api/emulation/awards"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(award(f.wardSecretary(), "PRAISE"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(auth.as(f.tdp1Secretary(), put("/api/emulation/ratings"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(rating(f.tdp2Member(), year, "GOOD"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(auth.as(f.tdp1Member(), put("/api/emulation/ratings"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(rating(f.tdp1Member(), year, "EXCELLENT"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMULATION_FORBIDDEN"));
    }

    @Test
    void ratingIsOnePerYearAndShowsOnTheBoard() throws Exception {
        mockMvc.perform(auth.as(f.tdp1Secretary(), put("/api/emulation/ratings"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(rating(f.tdp1Member(), year, "GOOD"))))
                .andExpect(status().isOk());
        mockMvc.perform(auth.as(f.tdp1Secretary(), put("/api/emulation/ratings"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(rating(f.tdp1Member(), year, "EXCELLENT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value("EXCELLENT"));

        mockMvc.perform(auth.as(f.tdp1Member(), get("/api/emulation/members/{id}", f.tdp1Member().getId())))
                .andExpect(jsonPath("$.ratings.length()").value(1))
                .andExpect(jsonPath("$.ratings[0].rating").value("EXCELLENT"));

        mockMvc.perform(auth.as(f.tdp1Secretary(), get("/api/emulation/ratings").param("year", String.valueOf(year))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[?(@.fullName == 'Doan vien TDP 1')].rating").value("EXCELLENT"));
        mockMvc.perform(auth.as(f.wardSecretary(), get("/api/emulation/summary").param("year", String.valueOf(year))))
                .andExpect(jsonPath("$.memberCount").value(7))
                .andExpect(jsonPath("$.ratingCounts.EXCELLENT").value(1))
                .andExpect(jsonPath("$.ratingCounts.UNRATED").value(6));

        mockMvc.perform(auth.as(f.tdp1Secretary(), put("/api/emulation/ratings"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(rating(f.tdp1Member(), year + 1, "GOOD"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RATING_YEAR"));
        mockMvc.perform(auth.as(f.tdp1Member(), get("/api/emulation/ratings").param("year", String.valueOf(year))))
                .andExpect(status().isForbidden());
    }

    private Map<String, Object> award(Member member, String form) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("memberId", member.getId());
        payload.put("form", form);
        payload.put("title", "Hoan thanh xuat sac nhiem vu");
        payload.put("decisionNumber", "12/QD-DTN");
        payload.put("decisionDate", AppTime.today().toString());
        payload.put("issuer", "Ban Thuong vu Doan phuong");
        return payload;
    }

    private Map<String, Object> rating(Member member, int ratingYear, String level) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("memberId", member.getId());
        payload.put("year", ratingYear);
        payload.put("rating", level);
        return payload;
    }
}
