package com.dotani.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dotani.support.IntegrationTest;
import com.dotani.support.TestData.Fixture;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

/** Instants are stored as UTC and exchanged as ISO-8601 with Z; calendar-date filters use Vietnam days. */
class TimeStorageIntegrationTest extends IntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void eventTimesAreStoredAndReturnedInUtcAndDateFilterUsesVietnamDays() throws Exception {
        Fixture f = data.standardFixture();
        // 2026-12-02 00:30 in Vietnam
        String eventId = createEvent(f, "2026-12-01T17:30:00Z", "2026-12-01T19:30:00Z");

        String stored = jdbcTemplate.queryForObject(
                "select date_format(start_time, '%Y-%m-%d %H:%i:%s') from events where id = ?", String.class, eventId);
        assertThat(stored).isEqualTo("2026-12-01 17:30:00");

        mockMvc.perform(auth.as(f.wardSecretary(), get("/api/events/{id}", eventId)))
                .andExpect(jsonPath("$.startTime").value("2026-12-01T17:30:00Z"));

        mockMvc.perform(auth.as(f.wardSecretary(), get("/api/events").param("date", "2026-12-02")))
                .andExpect(jsonPath("$.content.length()").value(1));
        mockMvc.perform(auth.as(f.wardSecretary(), get("/api/events").param("date", "2026-12-01")))
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void timesWithAnOffsetAreNormalizedToUtc() throws Exception {
        Fixture f = data.standardFixture();
        String eventId = createEvent(f, "2026-12-02T08:00:00+07:00", "2026-12-02T10:00:00+07:00");

        mockMvc.perform(auth.as(f.wardSecretary(), get("/api/events/{id}", eventId)))
                .andExpect(jsonPath("$.startTime").value("2026-12-02T01:00:00Z"));
    }

    @Test
    void createdAtIsWrittenAsUtc() {
        Fixture f = data.standardFixture();
        Long skewSeconds = jdbcTemplate.queryForObject(
                "select abs(timestampdiff(second, created_at, utc_timestamp())) from members where id = ?",
                Long.class, f.tdp1Member().getId());
        assertThat(skewSeconds).isLessThan(60);
    }

    private String createEvent(Fixture f, String start, String end) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", "Su kien");
        payload.put("type", "MEETING");
        payload.put("startTime", start);
        payload.put("endTime", end);
        payload.put("organizationId", f.ward().getId());
        payload.put("status", "PUBLISHED");
        var result = mockMvc.perform(auth.as(f.wardSecretary(), post("/api/events"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        return body(result).get("id").asText();
    }
}
