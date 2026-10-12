package com.dotani.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dotani.event.service.RegistrationDeadlineReminder;
import com.dotani.member.entity.Member;
import com.dotani.notification.entity.NotificationType;
import com.dotani.support.IntegrationTest;
import com.dotani.support.TestData.Fixture;
import java.time.Duration;
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

/** Notifications produced from domain events after the change commits. */
class DomainNotificationIntegrationTest extends IntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RegistrationDeadlineReminder registrationDeadlineReminder;

    private Fixture f;

    @BeforeEach
    void setUp() {
        f = data.standardFixture();
    }

    @Test
    void publishedPostNotifiesTheOrganizationExceptItsAuthor() throws Exception {
        createPost(f.ward().getId(), "DRAFT");
        assertThat(notificationsOf(f.tdp1Member(), NotificationType.POST_NEW)).isEmpty();

        String postId = createPost(f.tdp1().getId(), "PUBLISHED");

        assertThat(notificationsOf(f.tdp1Member(), NotificationType.POST_NEW)).containsExactly(postId);
        assertThat(notificationsOf(f.tdp2Member(), NotificationType.POST_NEW)).isEmpty();
        assertThat(notificationsOf(f.wardSecretary(), NotificationType.POST_NEW)).isEmpty();
    }

    @Test
    void publishingADraftPostNotifies() throws Exception {
        String postId = createPost(f.tdp1().getId(), "DRAFT");

        mockMvc.perform(auth.as(f.wardSecretary(), put("/api/posts/{id}", postId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(postPayload(f.tdp1().getId(), "PUBLISHED"))))
                .andExpect(status().isOk());

        assertThat(notificationsOf(f.tdp1Member(), NotificationType.POST_NEW)).containsExactly(postId);
    }

    @Test
    void scheduleChangeNotifiesOnlyMembersWhoAnswered() throws Exception {
        String eventId = createEvent(f.tdp1Secretary(), f.tdp1().getId(), "PUBLISHED", null);
        answer(f.tdp1Member(), eventId);

        Map<String, Object> changed = eventPayload(f.tdp1().getId(), "PUBLISHED", null);
        changed.put("startTime", "2026-12-05T01:00:00Z");
        changed.put("endTime", "2026-12-05T03:00:00Z");
        mockMvc.perform(auth.as(f.tdp1Secretary(), put("/api/events/{id}", eventId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(changed)))
                .andExpect(status().isOk());

        assertThat(notificationsOf(f.tdp1Member(), NotificationType.EVENT_UPDATED)).containsExactly(eventId);
        assertThat(notificationsOf(f.tdp1Deputy(), NotificationType.EVENT_UPDATED)).isEmpty();
    }

    @Test
    void cancellingOrDeletingNotifiesMembersWhoAnswered() throws Exception {
        String cancelled = createEvent(f.wardSecretary(), f.ward().getId(), "PUBLISHED", null);
        String deleted = createEvent(f.wardSecretary(), f.ward().getId(), "PUBLISHED", null);
        answer(f.tdp1Member(), cancelled);
        answer(f.tdp1Member(), deleted);

        mockMvc.perform(auth.as(f.wardSecretary(), put("/api/events/{id}", cancelled))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(eventPayload(f.ward().getId(), "CANCELLED", null))))
                .andExpect(status().isOk());
        mockMvc.perform(auth.as(f.wardSecretary(), delete("/api/events/{id}", deleted)))
                .andExpect(status().is2xxSuccessful());

        // the deleted event can no longer be opened, so its notification has no link
        assertThat(jdbcTemplate.queryForList(
                "select n.reference_id from notifications n join user_notifications un on un.notification_id = n.id"
                        + " where un.member_id = ? and n.notification_type = 'EVENT_CANCELLED' order by n.reference_id",
                String.class, f.tdp1Member().getId()))
                .containsExactlyInAnyOrder(cancelled, null);
    }

    @Test
    void deadlineReminderGoesToMembersWhoHaveNotAnsweredOnce() throws Exception {
        String deadline = Instant.now().plus(Duration.ofHours(12)).truncatedTo(ChronoUnit.SECONDS).toString();
        String eventId = createEvent(f.tdp1Secretary(), f.tdp1().getId(), "PUBLISHED", deadline);
        answer(f.tdp1Member(), eventId);

        assertThat(registrationDeadlineReminder.remindUpcomingDeadlines()).isEqualTo(1);
        assertThat(registrationDeadlineReminder.remindUpcomingDeadlines()).isZero();

        assertThat(notificationsOf(f.tdp1Deputy(), NotificationType.EVENT_DEADLINE)).containsExactly(eventId);
        assertThat(notificationsOf(f.tdp1Member(), NotificationType.EVENT_DEADLINE)).isEmpty();
        assertThat(notificationsOf(f.tdp2Member(), NotificationType.EVENT_DEADLINE)).isEmpty();
    }

    @Test
    void tdpSecretaryEventNotifiesThatTdp() throws Exception {
        String eventId = createEvent(f.tdp1Secretary(), f.tdp1().getId(), "PUBLISHED", null);

        assertThat(notificationsOf(f.tdp1Member(), NotificationType.EVENT_NEW)).containsExactly(eventId);
        assertThat(notificationsOf(f.tdp2Member(), NotificationType.EVENT_NEW)).isEmpty();
    }

    private List<String> notificationsOf(Member member, NotificationType type) {
        return jdbcTemplate.queryForList(
                "select n.reference_id from notifications n join user_notifications un on un.notification_id = n.id"
                        + " where un.member_id = ? and n.notification_type = ?",
                String.class, member.getId(), type.name());
    }

    private String createPost(String organizationId, String status) throws Exception {
        var result = mockMvc.perform(auth.as(f.wardSecretary(), post("/api/posts"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(postPayload(organizationId, status))))
                .andExpect(status().isCreated())
                .andReturn();
        return body(result).get("id").asText();
    }

    private Map<String, Object> postPayload(String organizationId, String status) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", "Bai viet");
        payload.put("content", "Noi dung");
        payload.put("type", "NEWS");
        payload.put("organizationId", organizationId);
        payload.put("status", status);
        return payload;
    }

    private String createEvent(Member creator, String organizationId, String status, String deadline) throws Exception {
        var result = mockMvc.perform(auth.as(creator, post("/api/events"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(eventPayload(organizationId, status, deadline))))
                .andExpect(status().isCreated())
                .andReturn();
        return body(result).get("id").asText();
    }

    private Map<String, Object> eventPayload(String organizationId, String status, String deadline) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", "Sinh hoat");
        payload.put("type", "MEETING");
        payload.put("location", "Nha van hoa");
        payload.put("startTime", "2026-12-01T01:00:00Z");
        payload.put("endTime", "2026-12-01T03:00:00Z");
        payload.put("registrationDeadline", deadline);
        payload.put("organizationId", organizationId);
        payload.put("status", status);
        return payload;
    }

    private void answer(Member member, String eventId) throws Exception {
        mockMvc.perform(auth.as(member, put("/api/events/{id}/participation", eventId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "GOING"))))
                .andExpect(status().isOk());
    }
}
