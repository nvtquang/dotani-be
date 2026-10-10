package com.dotani.notification;

import com.dotani.shared.security.Role;

import com.dotani.support.IntegrationTest;
import com.dotani.notification.dto.NotificationCreateRequest;
import com.dotani.notification.entity.NotificationType;
import com.dotani.notification.entity.ReferenceType;
import com.dotani.notification.service.NotificationManagementService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.dotani.notification.entity.Notification;
import com.dotani.notification.entity.UserNotification;
import com.dotani.notification.repository.NotificationRepository;
import com.dotani.notification.repository.UserNotificationRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

class NotificationIntegrationTest extends IntegrationTest {


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserNotificationRepository userNotificationRepository;

    @Autowired
    private NotificationManagementService notificationManagementService;

    @BeforeEach
    void setUp() {
        userNotificationRepository.deleteAll();
        notificationRepository.deleteAll();
    }

    @Test
    void createDeduplicatesRecipients() {
        var response = notificationManagementService.create(new NotificationCreateRequest(
                NotificationType.EVENT_NEW,
                "Su kien moi",
                "Moi doan vien dang ky tham gia",
                ReferenceType.EVENT,
                "event-1",
                List.of("member-1", "member-2", "member-1")
        ));

        assertThat(response.notificationId()).isNotBlank();
        assertThat(response.recipientCount()).isEqualTo(2);
        assertThat(notificationRepository.findAll()).hasSize(1);
        assertThat(userNotificationRepository.findAll()).hasSize(2);
    }

    @Test
    void memberOnlyReadsOwnNotificationsWithPagination() throws Exception {
        createNotification("member-1", "Tin cua member 1");
        createNotification("member-1", "Tin khac cua member 1");
        createNotification("member-2", "Tin cua member 2");

        mockMvc.perform(get("/api/notifications?page=0&size=10&sort=createdAt,desc")
                        .header(HttpHeaders.AUTHORIZATION, bearer("member-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].title").value(org.hamcrest.Matchers.containsString("member 1")))
                .andExpect(jsonPath("$.content[1].title").value(org.hamcrest.Matchers.containsString("member 1")));
    }

    @Test
    void unreadCountAndMarkReadWorkForCurrentMember() throws Exception {
        UserNotification first = createNotification("member-1", "Tin 1");
        createNotification("member-1", "Tin 2");

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header(HttpHeaders.AUTHORIZATION, bearer("member-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2));

        mockMvc.perform(put("/api/notifications/{id}/read", first.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer("member-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));

        mockMvc.perform(get("/api/notifications/unread-count")
                        .header(HttpHeaders.AUTHORIZATION, bearer("member-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    void memberCannotMarkAnotherMembersNotificationAsRead() throws Exception {
        UserNotification otherMemberNotification = createNotification("member-2", "Tin cua member 2");

        mockMvc.perform(put("/api/notifications/{id}/read", otherMemberNotification.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer("member-1")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));

        assertThat(userNotificationRepository.findById(otherMemberNotification.getId()).orElseThrow().getReadAt())
                .isNull();
    }

    @Test
    void readAllOnlyMarksCurrentMembersNotifications() throws Exception {
        createNotification("member-1", "Tin 1");
        createNotification("member-1", "Tin 2");
        createNotification("member-2", "Tin member 2");

        mockMvc.perform(put("/api/notifications/read-all")
                        .header(HttpHeaders.AUTHORIZATION, bearer("member-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));

        assertThat(userNotificationRepository.countByMemberIdAndReadAtIsNull("member-1")).isZero();
        assertThat(userNotificationRepository.countByMemberIdAndReadAtIsNull("member-2")).isEqualTo(1);
    }

    @Test
    void protectedEndpointWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private UserNotification createNotification(String memberId, String title) {
        Notification notification = new Notification();
        notification.setNotificationType(NotificationType.POST_NEW);
        notification.setTitle(title);
        notification.setContent("Noi dung thong bao");
        notification.setReferenceType(ReferenceType.POST);
        notification.setReferenceId("post-1");

        UserNotification userNotification = new UserNotification();
        userNotification.setMemberId(memberId);
        notification.addRecipient(userNotification);
        notificationRepository.saveAndFlush(notification);
        return userNotification;
    }

    private String bearer(String memberId) {
        return "Bearer " + token(memberId);
    }

    private String token(String memberId) {
        return auth.token(memberId + "-user", memberId, Role.MEMBER, "ward-thuong-cat", "tdp-1");
    }

}
