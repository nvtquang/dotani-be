package com.dotani.report;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dotani.member.entity.Gender;
import com.dotani.member.entity.Member;
import com.dotani.member.repository.MemberRepository;
import com.dotani.shared.time.AppTime;
import com.dotani.support.IntegrationTest;
import com.dotani.support.TestData.Fixture;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

/** Báo cáo thống kê: membership demographics and event participation, by scope. */
class ReportIntegrationTest extends IntegrationTest {

    @Autowired
    private MemberRepository memberRepository;

    private Fixture f;

    @BeforeEach
    void setUp() {
        f = data.standardFixture();
        LocalDate today = AppTime.today();
        profile(f.tdp1Member(), Gender.FEMALE, today.minusYears(18));
        profile(f.tdp1Secretary(), Gender.MALE, today.minusYears(27));
        profile(f.tdp2Member(), Gender.MALE, today.minusYears(22));
    }

    @Test
    void wardReportCoversEveryTdp() throws Exception {
        mockMvc.perform(auth.as(f.wardSecretary(), get("/api/reports/members")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMembers").value(7))
                .andExpect(jsonPath("$.activeMembers").value(7))
                .andExpect(jsonPath("$.genderCounts.MALE").value(2))
                .andExpect(jsonPath("$.genderCounts.FEMALE").value(1))
                .andExpect(jsonPath("$.genderCounts.UNKNOWN").value(4))
                .andExpect(jsonPath("$.ageGroups[?(@.key == '16_20')].count").value(1))
                .andExpect(jsonPath("$.ageGroups[?(@.key == '21_25')].count").value(1))
                .andExpect(jsonPath("$.ageGroups[?(@.key == '26_30')].count").value(1))
                .andExpect(jsonPath("$.byOrganization[?(@.organizationName == 'Chi doan TDP 1')].total").value(3))
                .andExpect(jsonPath("$.byOrganization[?(@.organizationName == 'Chi doan TDP 2')].total").value(2));
    }

    @Test
    void tdpOfficerReportsOnOwnTdpOnly() throws Exception {
        mockMvc.perform(auth.as(f.tdp1Secretary(), get("/api/reports/members")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMembers").value(3))
                .andExpect(jsonPath("$.byOrganization.length()").value(1));
        mockMvc.perform(auth.as(f.tdp1Secretary(), get("/api/reports/members").param("organizationId", f.tdp2().getId())))
                .andExpect(status().isForbidden());
        mockMvc.perform(auth.as(f.tdp1Member(), get("/api/reports/members")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("REPORT_FORBIDDEN"));
    }

    @Test
    void participationRateIsGoingOverInvited() throws Exception {
        int year = AppTime.today().getYear();
        String start = year + "-12-01T01:00:00Z";
        var created = mockMvc.perform(auth.as(f.tdp1Secretary(), post("/api/events"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(event(f.tdp1().getId(), start))))
                .andExpect(status().isCreated())
                .andReturn();
        String eventId = body(created).get("id").asText();
        mockMvc.perform(auth.as(f.tdp1Member(), put("/api/events/{id}/participation", eventId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "GOING"))))
                .andExpect(status().isOk());

        // 3 active members in TDP 1 were invited, 1 is going
        mockMvc.perform(auth.as(f.wardSecretary(), get("/api/reports/participation")
                        .param("year", String.valueOf(year)).param("granularity", "QUARTER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periods.length()").value(4))
                .andExpect(jsonPath("$.periods[3].eventCount").value(1))
                .andExpect(jsonPath("$.periods[3].invitedCount").value(3))
                .andExpect(jsonPath("$.periods[3].goingCount").value(1))
                .andExpect(jsonPath("$.periods[3].rate").value(0.333))
                .andExpect(jsonPath("$.periods[0].rate").doesNotExist())
                .andExpect(jsonPath("$.total.eventCount").value(1))
                .andExpect(jsonPath("$.events[0].eventId").value(eventId));

        // TDP 2 was not invited to a TDP 1 event
        mockMvc.perform(auth.as(f.tdp2Secretary(), get("/api/reports/participation").param("year", String.valueOf(year))))
                .andExpect(jsonPath("$.periods.length()").value(12))
                .andExpect(jsonPath("$.total.eventCount").value(0));
    }

    private void profile(Member member, Gender gender, LocalDate dateOfBirth) {
        Member saved = memberRepository.findById(member.getId()).orElseThrow();
        saved.setGender(gender);
        saved.setDateOfBirth(dateOfBirth);
        memberRepository.save(saved);
    }

    private Map<String, Object> event(String organizationId, String start) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", "Ra quan thanh nien");
        payload.put("type", "ACTIVITY");
        payload.put("startTime", start);
        payload.put("endTime", start.replace("01:00", "05:00"));
        payload.put("organizationId", organizationId);
        payload.put("status", "PUBLISHED");
        return payload;
    }
}
