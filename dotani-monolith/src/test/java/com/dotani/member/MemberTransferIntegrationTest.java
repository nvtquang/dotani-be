package com.dotani.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dotani.member.entity.Member;
import com.dotani.member.entity.MemberRole;
import com.dotani.member.repository.MemberRepository;
import com.dotani.support.IntegrationTest;
import com.dotani.support.TestData.Fixture;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

/** Chuyển sinh hoạt: request, approval by the receiving side, history. */
class MemberTransferIntegrationTest extends IntegrationTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Fixture f;

    @BeforeEach
    void setUp() {
        f = data.standardFixture();
    }

    @Test
    void memberRequestsAndReceivingTdpApproves() throws Exception {
        String transferId = request(f.tdp1Member(), null, f.tdp2().getId());

        mockMvc.perform(auth.as(f.tdp1Member(), post("/api/member-transfers"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("toOrganizationId", f.tdp2().getId()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRANSFER_ALREADY_PENDING"));

        // the TDP the member leaves can see the request but does not decide it
        mockMvc.perform(auth.as(f.tdp1Secretary(), get("/api/member-transfers/{id}", transferId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canDecide").value(false));
        mockMvc.perform(auth.as(f.tdp1Secretary(), post("/api/member-transfers/{id}/approve", transferId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TRANSFER_DECISION_FORBIDDEN"));

        mockMvc.perform(auth.as(f.tdp2Secretary(), post("/api/member-transfers/{id}/approve", transferId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("note", "Dong y tiep nhan"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.decidedByName").value("Bi thu TDP 2"));

        assertThat(memberRepository.findById(f.tdp1Member().getId()).orElseThrow().getOrganization().getId())
                .isEqualTo(f.tdp2().getId());
        assertThat(notificationTypes(f.tdp1Member())).contains("TRANSFER_DECIDED");
        // officers of the receiving TDP and of the ward were asked to decide
        assertThat(notificationTypes(f.tdp2Secretary())).contains("TRANSFER_REQUESTED");
        assertThat(notificationTypes(f.wardDeputy())).contains("TRANSFER_REQUESTED");
        assertThat(notificationTypes(f.tdp1Secretary())).doesNotContain("TRANSFER_REQUESTED");

        mockMvc.perform(auth.as(f.wardSecretary(), get("/api/member-transfers").param("memberId", f.tdp1Member().getId())))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].fromOrganizationName").value("Chi doan TDP 1"))
                .andExpect(jsonPath("$.content[0].toOrganizationName").value("Chi doan TDP 2"));
    }

    @Test
    void transferredTdpOfficerBecomesMember() throws Exception {
        String transferId = request(f.wardSecretary(), f.tdp1Deputy().getId(), f.tdp2().getId());
        mockMvc.perform(auth.as(f.wardSecretary(), post("/api/member-transfers/{id}/approve", transferId)))
                .andExpect(status().isOk());

        Member moved = memberRepository.findById(f.tdp1Deputy().getId()).orElseThrow();
        assertThat(moved.getMemberRole()).isEqualTo(MemberRole.MEMBER);
        assertThat(moved.getOrganization().getId()).isEqualTo(f.tdp2().getId());
    }

    @Test
    void rejectAndCancel() throws Exception {
        String rejected = request(f.tdp1Member(), null, f.tdp2().getId());
        mockMvc.perform(auth.as(f.wardDeputy(), post("/api/member-transfers/{id}/reject", rejected)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
        mockMvc.perform(auth.as(f.wardDeputy(), post("/api/member-transfers/{id}/approve", rejected)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRANSFER_NOT_PENDING"));

        String cancelled = request(f.tdp1Member(), null, f.tdp2().getId());
        mockMvc.perform(auth.as(f.tdp1Member(), post("/api/member-transfers/{id}/cancel", cancelled)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(memberRepository.findById(f.tdp1Member().getId()).orElseThrow().getOrganization().getId())
                .isEqualTo(f.tdp1().getId());
    }

    @Test
    void invalidRequestsAreRefused() throws Exception {
        mockMvc.perform(auth.as(f.tdp1Member(), post("/api/member-transfers"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("toOrganizationId", f.tdp1().getId()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TRANSFER_TARGET"));
        mockMvc.perform(auth.as(f.tdp1Member(), post("/api/member-transfers"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("toOrganizationId", f.ward().getId()))))
                .andExpect(status().isBadRequest());
        // a member cannot move someone else, a TDP officer cannot move another TDP's member
        mockMvc.perform(auth.as(f.tdp1Member(), post("/api/member-transfers"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("memberId", f.tdp2Member().getId(), "toOrganizationId", f.tdp1().getId()))))
                .andExpect(status().isForbidden());
        mockMvc.perform(auth.as(f.tdp1Secretary(), post("/api/member-transfers"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("memberId", f.tdp2Member().getId(), "toOrganizationId", f.tdp1().getId()))))
                .andExpect(status().isForbidden());
        // other members' transfers are not visible
        String transferId = request(f.tdp1Member(), null, f.tdp2().getId());
        mockMvc.perform(auth.as(f.tdp2Member(), get("/api/member-transfers/{id}", transferId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void directProfileEditIsRecordedInHistory() throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", f.tdp1Member().getUserId());
        payload.put("fullName", f.tdp1Member().getFullName());
        payload.put("organizationId", f.tdp2().getId());
        payload.put("memberStatus", "ACTIVE");
        mockMvc.perform(auth.as(f.wardSecretary(), put("/api/members/{id}", f.tdp1Member().getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload)))
                .andExpect(status().isOk());

        mockMvc.perform(auth.as(f.tdp1Member(), get("/api/member-transfers")))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("APPROVED"))
                .andExpect(jsonPath("$.content[0].toOrganizationId").value(f.tdp2().getId()));
    }

    private java.util.List<String> notificationTypes(Member member) {
        return jdbcTemplate.queryForList(
                "select n.notification_type from notifications n join user_notifications un on un.notification_id = n.id"
                        + " where un.member_id = ?",
                String.class, member.getId());
    }

    private String request(Member actor, String memberId, String toOrganizationId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("memberId", memberId);
        payload.put("toOrganizationId", toOrganizationId);
        payload.put("reason", "Chuyen noi o");
        var result = mockMvc.perform(auth.as(actor, post("/api/member-transfers"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        return body(result).get("id").asText();
    }
}
