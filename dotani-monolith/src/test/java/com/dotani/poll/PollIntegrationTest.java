package com.dotani.poll;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dotani.member.entity.Member;
import com.dotani.support.IntegrationTest;
import com.dotani.support.TestData.Fixture;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

/** Khảo sát / biểu quyết: eligibility, one vote, result visibility, secret ballots. */
class PollIntegrationTest extends IntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Fixture f;

    @BeforeEach
    void setUp() {
        f = data.standardFixture();
    }

    @Test
    void eligibleMemberVotesOnceAndSeesResultsAfterVoting() throws Exception {
        JsonNode poll = create(f.tdp1Secretary(), f.tdp1().getId(), false, true);
        String pollId = poll.get("id").asText();
        String optionId = poll.get("options").get(0).get("id").asText();

        mockMvc.perform(auth.as(f.tdp1Member(), get("/api/polls/{id}", pollId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.eligibleCount").value(3))
                .andExpect(jsonPath("$.resultsVisible").value(false))
                .andExpect(jsonPath("$.options[0].voteCount").doesNotExist());

        vote(f.tdp1Member(), pollId, List.of(optionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasVoted").value(true))
                .andExpect(jsonPath("$.resultsVisible").value(true))
                .andExpect(jsonPath("$.ballotCount").value(1))
                .andExpect(jsonPath("$.options[0].voteCount").value(1))
                .andExpect(jsonPath("$.myOptionIds[0]").value(optionId));
        vote(f.tdp1Member(), pollId, List.of(optionId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("POLL_ALREADY_VOTED"));

        // another TDP neither sees nor votes
        mockMvc.perform(auth.as(f.tdp2Member(), get("/api/polls/{id}", pollId))).andExpect(status().isForbidden());
        vote(f.tdp2Member(), pollId, List.of(optionId)).andExpect(status().isForbidden());

        // an open (non-secret) vote shows officers who chose what
        mockMvc.perform(auth.as(f.tdp1Secretary(), get("/api/polls/{id}", pollId)))
                .andExpect(jsonPath("$.options[0].voters[0].fullName").value("Doan vien TDP 1"));
        assertThat(notificationCount(f.tdp1Deputy())).isEqualTo(1);
    }

    @Test
    void secretBallotStoresNoVoter() throws Exception {
        JsonNode poll = create(f.wardSecretary(), f.ward().getId(), true, true);
        String pollId = poll.get("id").asText();
        String optionId = poll.get("options").get(1).get("id").asText();

        vote(f.tdp2Member(), pollId, List.of(optionId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.myOptionIds.length()").value(0));

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from poll_answers where poll_id = ? and member_id is null", Integer.class, pollId)).isEqualTo(1);
        mockMvc.perform(auth.as(f.wardSecretary(), get("/api/polls/{id}", pollId)))
                .andExpect(jsonPath("$.options[1].voteCount").value(1))
                .andExpect(jsonPath("$.options[1].voters").doesNotExist())
                .andExpect(jsonPath("$.eligibleCount").value(7));
    }

    @Test
    void draftsAreHiddenFromMembersAndOptionsLockOnceOpen() throws Exception {
        JsonNode draft = create(f.wardSecretary(), f.ward().getId(), false, false);
        String pollId = draft.get("id").asText();

        mockMvc.perform(auth.as(f.tdp1Member(), get("/api/polls"))).andExpect(jsonPath("$.content.length()").value(0));
        mockMvc.perform(auth.as(f.tdp1Member(), get("/api/polls/{id}", pollId))).andExpect(status().isNotFound());

        mockMvc.perform(auth.as(f.wardSecretary(), post("/api/polls/{id}/open", pollId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));

        Map<String, Object> changed = payload(f.ward().getId(), false, false);
        changed.put("options", List.of("Dong y", "Khong dong y", "Y kien khac"));
        mockMvc.perform(auth.as(f.wardSecretary(), put("/api/polls/{id}", pollId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(changed)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("POLL_LOCKED"));

        String first = draft.get("options").get(0).get("id").asText();
        String second = draft.get("options").get(1).get("id").asText();
        vote(f.tdp1Member(), pollId, List.of(first, second))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_POLL_OPTIONS"));

        mockMvc.perform(auth.as(f.wardSecretary(), post("/api/polls/{id}/close", pollId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
        vote(f.tdp1Member(), pollId, List.of(first))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("POLL_NOT_OPEN"));
        // after closing, results are public
        mockMvc.perform(auth.as(f.tdp1Member(), get("/api/polls/{id}", pollId)))
                .andExpect(jsonPath("$.resultsVisible").value(true));
    }

    @Test
    void membersCannotCreatePolls() throws Exception {
        mockMvc.perform(auth.as(f.tdp1Member(), post("/api/polls"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload(f.tdp1().getId(), false, true))))
                .andExpect(status().isForbidden());
        mockMvc.perform(auth.as(f.tdp1Secretary(), post("/api/polls"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload(f.tdp2().getId(), false, true))))
                .andExpect(status().isForbidden());
    }

    private long notificationCount(Member member) {
        return jdbcTemplate.queryForObject(
                "select count(*) from notifications n join user_notifications un on un.notification_id = n.id"
                        + " where un.member_id = ? and n.notification_type = 'POLL_OPENED'",
                Long.class, member.getId());
    }

    private JsonNode create(Member creator, String organizationId, boolean anonymous, boolean open) throws Exception {
        var result = mockMvc.perform(auth.as(creator, post("/api/polls"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload(organizationId, anonymous, open))))
                .andExpect(status().isCreated())
                .andReturn();
        return body(result);
    }

    private org.springframework.test.web.servlet.ResultActions vote(Member member, String pollId, List<String> optionIds) throws Exception {
        return mockMvc.perform(auth.as(member, post("/api/polls/{id}/votes", pollId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("optionIds", optionIds))));
    }

    private Map<String, Object> payload(String organizationId, boolean anonymous, boolean open) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", "Bieu quyet nhan su");
        payload.put("organizationId", organizationId);
        payload.put("anonymous", anonymous);
        payload.put("maxChoices", 1);
        payload.put("resultsVisibility", "AFTER_VOTE");
        payload.put("closesAt", Instant.now().plus(2, ChronoUnit.DAYS).toString());
        payload.put("options", List.of("Dong y", "Khong dong y"));
        payload.put("open", open);
        return payload;
    }
}
