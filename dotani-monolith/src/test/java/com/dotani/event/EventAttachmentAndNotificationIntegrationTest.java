package com.dotani.event;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dotani.member.entity.Member;
import com.dotani.member.entity.MemberRole;
import com.dotani.member.entity.MemberStatus;
import com.dotani.member.entity.OrganizationUnit;
import com.dotani.support.IntegrationTest;
import com.dotani.support.TestData.Fixture;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

class EventAttachmentAndNotificationIntegrationTest extends IntegrationTest {

    private Fixture f;

    @BeforeEach
    void setUp() {
        f = data.standardFixture();
    }

    @Test
    void wardLevelEventNotifiesActiveMembersOfEveryTdpUnderTheWard() throws Exception {
        Member inactive = data.member("Doan vien da nghi", f.tdp2(), MemberRole.MEMBER);
        inactive.setMemberStatus(MemberStatus.INACTIVE);
        data.save(inactive);

        createEvent(f.ward());

        // ward officers + both TDPs, but not the inactive member
        for (Member recipient : new Member[] {f.wardDeputy(), f.tdp1Member(), f.tdp2Member(), f.tdp2Secretary()}) {
            mockMvc.perform(auth.as(recipient, get("/api/notifications/unread-count")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.count").value(1));
        }
        mockMvc.perform(auth.as(inactive, get("/api/notifications/unread-count")))
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void tdpLevelEventNotifiesOnlyThatTdp() throws Exception {
        String eventId = createEvent(f.tdp1());

        mockMvc.perform(auth.as(f.tdp1Member(), get("/api/notifications")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].notificationType").value("EVENT_NEW"))
                .andExpect(jsonPath("$.content[0].referenceType").value("EVENT"))
                .andExpect(jsonPath("$.content[0].referenceId").value(eventId));

        mockMvc.perform(auth.as(f.tdp2Member(), get("/api/notifications/unread-count")))
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void officerCanUploadImageAndDocumentAttachmentsAndDeleteThem() throws Exception {
        String eventId = createEvent(f.tdp1());
        MockMultipartFile image = new MockMultipartFile("files", "poster.png", "image/png", new byte[] {1, 2, 3});
        MockMultipartFile pdf = new MockMultipartFile("files", "ke-hoach.pdf", "application/pdf", new byte[] {4, 5});

        var result = mockMvc.perform(auth.as(f.wardSecretary(), multipart("/api/events/{id}/attachments", eventId)
                        .file(image)
                        .file(pdf)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attachments.length()").value(2))
                .andExpect(jsonPath("$.attachments[?(@.fileName == 'poster.png')].attachmentKind").value("IMAGE"))
                .andExpect(jsonPath("$.attachments[?(@.fileName == 'ke-hoach.pdf')].attachmentKind").value("FILE"))
                .andReturn();
        String attachmentId = body(result).get("attachments").get(0).get("id").asText();

        mockMvc.perform(auth.as(f.tdp1Member(), get("/api/events/{id}", eventId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attachments.length()").value(2));

        mockMvc.perform(auth.as(f.wardSecretary(), delete("/api/events/{id}/attachments/{attachmentId}", eventId, attachmentId)))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(auth.as(f.wardSecretary(), get("/api/events/{id}", eventId)))
                .andExpect(jsonPath("$.attachments.length()").value(1));
    }

    @Test
    void memberCannotUploadAttachmentsAndExecutablesAreRejected() throws Exception {
        String eventId = createEvent(f.tdp1());
        MockMultipartFile image = new MockMultipartFile("files", "poster.png", "image/png", new byte[] {1});

        mockMvc.perform(auth.as(f.tdp1Member(), multipart("/api/events/{id}/attachments", eventId).file(image)))
                .andExpect(status().isForbidden());

        MockMultipartFile executable = new MockMultipartFile("files", "setup.exe", "application/x-msdownload", new byte[] {1});
        mockMvc.perform(auth.as(f.wardSecretary(), multipart("/api/events/{id}/attachments", eventId).file(executable)))
                .andExpect(status().isBadRequest());
    }

    private String createEvent(OrganizationUnit organization) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", "Su kien " + organization.getCode());
        payload.put("description", "Mo ta");
        payload.put("type", "MEETING");
        payload.put("location", "Hoi truong");
        payload.put("startTime", LocalDateTime.now().plusDays(7).withNano(0).toString());
        payload.put("endTime", LocalDateTime.now().plusDays(7).plusHours(2).withNano(0).toString());
        payload.put("organizationId", organization.getId());
        payload.put("status", "PUBLISHED");

        var result = mockMvc.perform(auth.as(f.wardSecretary(), post("/api/events"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        return body(result).get("id").asText();
    }
}
