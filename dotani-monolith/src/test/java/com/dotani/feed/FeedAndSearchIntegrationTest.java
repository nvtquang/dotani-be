package com.dotani.feed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dotani.content.entity.Post;
import com.dotani.content.entity.PostStatus;
import com.dotani.content.entity.PostType;
import com.dotani.content.repository.PostRepository;
import com.dotani.event.entity.Event;
import com.dotani.event.entity.EventParticipation;
import com.dotani.event.entity.EventStatus;
import com.dotani.event.entity.EventType;
import com.dotani.event.entity.ParticipationStatus;
import com.dotani.event.repository.EventParticipationRepository;
import com.dotani.event.repository.EventRepository;
import com.dotani.member.entity.Member;
import com.dotani.support.IntegrationTest;
import com.dotani.support.TestData.Fixture;
import com.fasterxml.jackson.databind.JsonNode;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class FeedAndSearchIntegrationTest extends IntegrationTest {

    private static final Instant BASE = Instant.parse("2026-10-01T03:00:00Z");

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventParticipationRepository participationRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Fixture f;

    @BeforeEach
    void setUp() {
        f = data.standardFixture();
    }

    @Test
    void feedMixesEventsAndPostsNewestFirstAndPagesWithCursor() throws Exception {
        // created at BASE+1h .. BASE+5h, alternating event/post, all in TDP 1
        for (int i = 1; i <= 5; i++) {
            if (i % 2 == 1) {
                createdAt("events", event("Su kien " + i, f.tdp1().getId(), EventStatus.PUBLISHED, 3).getId(), i);
            } else {
                createdAt("posts", post("Bai viet " + i, f.tdp1().getId(), PostStatus.PUBLISHED).getId(), i);
            }
        }

        JsonNode first = body(mockMvc.perform(auth.as(f.tdp1Member(), get("/api/feed").param("size", "3")))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(titles(first)).containsExactly("Su kien 5", "Bai viet 4", "Su kien 3");
        assertThat(first.get("items").get(0).get("kind").asText()).isEqualTo("EVENT");
        assertThat(first.get("items").get(1).get("kind").asText()).isEqualTo("POST");

        JsonNode second = body(mockMvc.perform(auth.as(f.tdp1Member(), get("/api/feed")
                        .param("size", "3")
                        .param("before", first.get("nextCursor").asText())))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(titles(second)).containsExactly("Bai viet 2", "Su kien 1");
        assertThat(second.get("nextCursor").isNull()).isTrue();

        JsonNode postsOnly = body(mockMvc.perform(auth.as(f.tdp1Member(), get("/api/feed").param("kind", "POST")))
                .andReturn());
        assertThat(titles(postsOnly)).containsExactly("Bai viet 4", "Bai viet 2");
    }

    @Test
    void feedHidesDraftsAndOtherTdpsFromMembers() throws Exception {
        event("Nhap TDP 1", f.tdp1().getId(), EventStatus.DRAFT, 3);
        post("Nhap bai TDP 1", f.tdp1().getId(), PostStatus.DRAFT);
        event("Su kien TDP 2", f.tdp2().getId(), EventStatus.PUBLISHED, 3);
        event("Su kien phuong", f.ward().getId(), EventStatus.PUBLISHED, 3);

        JsonNode member = body(mockMvc.perform(auth.as(f.tdp1Member(), get("/api/feed"))).andReturn());
        assertThat(titles(member)).containsExactly("Su kien phuong");

        JsonNode secretary = body(mockMvc.perform(auth.as(f.tdp1Secretary(), get("/api/feed"))).andReturn());
        assertThat(titles(secretary)).contains("Nhap TDP 1", "Nhap bai TDP 1").doesNotContain("Su kien TDP 2");
    }

    @Test
    void searchFindsMembersEventsAndPostsWithinScope() throws Exception {
        event("Hien mau nhan dao", f.tdp1().getId(), EventStatus.PUBLISHED, 3);
        event("Hien mau TDP 2", f.tdp2().getId(), EventStatus.PUBLISHED, 3);
        post("Tong ket hien mau", f.tdp1().getId(), PostStatus.PUBLISHED);

        mockMvc.perform(auth.as(f.tdp1Member(), get("/api/search").param("q", "hien MAU")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events.length()").value(1))
                .andExpect(jsonPath("$.events[0].title").value("Hien mau nhan dao"))
                .andExpect(jsonPath("$.posts[0].title").value("Tong ket hien mau"));

        mockMvc.perform(auth.as(f.tdp1Member(), get("/api/search").param("q", "Bi thu TDP")))
                .andExpect(jsonPath("$.members.length()").value(3))
                // directory fields only, nothing sensitive
                .andExpect(jsonPath("$.members[0].phone").doesNotExist())
                .andExpect(jsonPath("$.members[0].email").doesNotExist());

        // too short: nothing, not everything
        mockMvc.perform(auth.as(f.tdp1Member(), get("/api/search").param("q", "h")))
                .andExpect(jsonPath("$.members.length()").value(0))
                .andExpect(jsonPath("$.events.length()").value(0));
    }

    @Test
    void searchIgnoresVietnameseDiacriticsIncludingDStrokeAndTreatsWildcardsLiterally() throws Exception {
        event("Đại hội chi đoàn", f.tdp1().getId(), EventStatus.PUBLISHED, 3);
        event("Hoạt động tình nguyện", f.tdp1().getId(), EventStatus.PUBLISHED, 3);

        for (String query : new String[] {"dai hoi", "ĐẠI HỘI", "hoat dong", "tinh nguyen"}) {
            mockMvc.perform(auth.as(f.tdp1Member(), get("/api/search").param("q", query)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.events.length()").value(1));
        }

        mockMvc.perform(auth.as(f.tdp1Member(), get("/api/search").param("q", "%%")))
                .andExpect(jsonPath("$.events.length()").value(0))
                .andExpect(jsonPath("$.members.length()").value(0));
    }

    @Test
    void eventListFiltersByWhenRangeAndParticipationAndReportsGoingCount() throws Exception {
        Event past = event("Da qua", f.tdp1().getId(), EventStatus.COMPLETED, -3);
        Event soon = event("Sap toi", f.tdp1().getId(), EventStatus.PUBLISHED, 2);
        Event later = event("Thang sau", f.tdp1().getId(), EventStatus.PUBLISHED, 40);
        going(soon, f.tdp1Member());
        going(soon, f.tdp1Secretary());
        going(past, f.tdp1Member());

        assertThat(listTitles(f.tdp1Member(), "when", "PAST")).containsExactly("Da qua");
        assertThat(listTitles(f.tdp1Member(), "when", "UPCOMING")).containsExactlyInAnyOrder("Sap toi", "Thang sau");
        assertThat(listTitles(f.tdp1Member(), "upcoming", "true")).containsExactlyInAnyOrder("Sap toi", "Thang sau");
        assertThat(listTitles(f.tdp1Member(), "participating", "true")).containsExactlyInAnyOrder("Da qua", "Sap toi");
        assertThat(listTitles(f.tdp1Secretary(), "participating", "true")).containsExactly("Sap toi");

        Instant from = Instant.now().plus(Duration.ofDays(1));
        Instant to = Instant.now().plus(Duration.ofDays(30));
        JsonNode range = body(mockMvc.perform(auth.as(f.tdp1Member(), get("/api/events")
                        .param("from", from.toString())
                        .param("to", to.toString())))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(range.get("content")).hasSize(1);
        assertThat(range.get("content").get(0).get("title").asText()).isEqualTo("Sap toi");
        assertThat(range.get("content").get(0).get("goingCount").asLong()).isEqualTo(2);

        mockMvc.perform(auth.as(f.tdp1Member(), get("/api/events/{id}", later.getId())))
                .andExpect(jsonPath("$.goingCount").value(0));
    }

    private List<String> listTitles(Member member, String param, String value) throws Exception {
        JsonNode page = body(mockMvc.perform(auth.as(member, get("/api/events").param(param, value)))
                .andExpect(status().isOk())
                .andReturn());
        List<String> titles = new ArrayList<>();
        page.get("content").forEach(item -> titles.add(item.get("title").asText()));
        return titles;
    }

    private static List<String> titles(JsonNode feed) {
        List<String> titles = new ArrayList<>();
        feed.get("items").forEach(item -> titles.add(
                item.get(item.get("kind").asText().equals("EVENT") ? "event" : "post").get("title").asText()));
        return titles;
    }

    private void createdAt(String table, String id, int hours) {
        jdbcTemplate.update("update " + table + " set created_at = ? where id = ?",
                Timestamp.from(BASE.plus(Duration.ofHours(hours))), id);
    }

    private Event event(String title, String organizationId, EventStatus status, int daysFromNow) {
        Instant start = Instant.now().plus(Duration.ofDays(daysFromNow));
        Event event = new Event();
        event.setTitle(title);
        event.setType(EventType.ACTIVITY);
        event.setStartTime(start);
        event.setEndTime(start.plus(Duration.ofHours(2)));
        event.setOrganizationId(organizationId);
        event.setMaxParticipants(30);
        event.setStatus(status);
        event.setCreatedBy("seed-user");
        return eventRepository.save(event);
    }

    private Post post(String title, String organizationId, PostStatus status) {
        Post post = new Post();
        post.setTitle(title);
        post.setContent(title + " content");
        post.setType(PostType.NEWS);
        post.setOrganizationId(organizationId);
        post.setAuthorId(f.wardSecretary().getId());
        post.setStatus(status);
        return postRepository.save(post);
    }

    private void going(Event event, Member member) {
        EventParticipation participation = new EventParticipation();
        participation.setEvent(event);
        participation.setMemberId(member.getId());
        participation.setStatus(ParticipationStatus.GOING);
        participationRepository.save(participation);
    }
}
