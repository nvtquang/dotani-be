package com.dotani.fee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dotani.member.entity.Member;
import com.dotani.support.IntegrationTest;
import com.dotani.support.TestData.Fixture;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

/** Đoàn phí: periods, payments recorded by officers in scope, reminders, members' own status. */
class FeeIntegrationTest extends IntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Fixture f;

    @BeforeEach
    void setUp() {
        f = data.standardFixture();
    }

    @Test
    void memberSeesWardPeriodWithOwnStatusAndCollector() throws Exception {
        String periodId = createPeriod(f.wardSecretary(), f.ward().getId(), f.wardSecretary().getId());

        mockMvc.perform(auth.as(f.tdp1Member(), get("/api/fees/periods")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(periodId))
                .andExpect(jsonPath("$.content[0].myPayment.paid").value(false))
                .andExpect(jsonPath("$.content[0].myPayment.amount").value(60000))
                .andExpect(jsonPath("$.content[0].summary").doesNotExist())
                .andExpect(jsonPath("$.content[0].collector.fullName").value("Bi thu phuong"))
                .andExpect(jsonPath("$.content[0].canManage").value(false));

        // the ward period applies to all 7 active members of the ward
        mockMvc.perform(auth.as(f.wardSecretary(), get("/api/fees/periods/{id}", periodId)))
                .andExpect(jsonPath("$.summary.expectedCount").value(7))
                .andExpect(jsonPath("$.summary.paidCount").value(0))
                .andExpect(jsonPath("$.summary.expectedAmount").value(420000));
    }

    @Test
    void tdpOfficerRecordsPaymentsOfOwnTdpOnly() throws Exception {
        String periodId = createPeriod(f.wardSecretary(), f.ward().getId(), null);

        mockMvc.perform(auth.as(f.tdp1Secretary(), post("/api/fees/periods/{id}/payments", periodId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("memberId", f.tdp1Member().getId(), "method", "CASH"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paid").value(true))
                .andExpect(jsonPath("$.amount").value(60000))
                .andExpect(jsonPath("$.recordedByName").value("Bi thu TDP 1"));

        mockMvc.perform(auth.as(f.tdp1Secretary(), post("/api/fees/periods/{id}/payments", periodId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("memberId", f.tdp2Member().getId(), "method", "CASH"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OUT_OF_SCOPE"));

        mockMvc.perform(auth.as(f.tdp1Deputy(), post("/api/fees/periods/{id}/payments", periodId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("memberId", f.tdp1Member().getId(), "method", "TRANSFER"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FEE_ALREADY_PAID"));

        // TDP officers see the members of their TDP only
        mockMvc.perform(auth.as(f.tdp1Secretary(), get("/api/fees/periods/{id}/members", periodId)))
                .andExpect(jsonPath("$.totalElements").value(3));
        mockMvc.perform(auth.as(f.tdp1Secretary(), get("/api/fees/periods/{id}/members", periodId).param("paid", "false")))
                .andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(auth.as(f.tdp1Member(), get("/api/fees/periods/{id}", periodId)))
                .andExpect(jsonPath("$.myPayment.paid").value(true))
                .andExpect(jsonPath("$.myPayment.method").value("CASH"));
    }

    @Test
    void membersCannotManageAndPeriodsWithPaymentsCannotBeDeleted() throws Exception {
        mockMvc.perform(auth.as(f.tdp1Member(), post("/api/fees/periods"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(periodPayload(f.tdp1().getId(), null))))
                .andExpect(status().isForbidden());

        String periodId = createPeriod(f.tdp1Secretary(), f.tdp1().getId(), f.tdp1Secretary().getId());
        mockMvc.perform(auth.as(f.tdp1Member(), get("/api/fees/periods/{id}/members", periodId)))
                .andExpect(status().isForbidden());
        // another TDP cannot see the period at all
        mockMvc.perform(auth.as(f.tdp2Member(), get("/api/fees/periods/{id}", periodId)))
                .andExpect(status().isForbidden());

        pay(f.tdp1Secretary(), periodId, f.tdp1Member());
        mockMvc.perform(auth.as(f.tdp1Secretary(), delete("/api/fees/periods/{id}", periodId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FEE_PERIOD_HAS_PAYMENTS"));
    }

    @Test
    void collectorMustBelongToThePeriod() throws Exception {
        mockMvc.perform(auth.as(f.tdp1Secretary(), post("/api/fees/periods"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(periodPayload(f.tdp1().getId(), f.tdp2Secretary().getId()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FEE_COLLECTOR_INVALID"));
    }

    @Test
    void reminderNotifiesUnpaidMembersOncePerHour() throws Exception {
        String periodId = createPeriod(f.tdp1Secretary(), f.tdp1().getId(), null);
        pay(f.tdp1Secretary(), periodId, f.tdp1Member());

        mockMvc.perform(auth.as(f.tdp1Secretary(), post("/api/fees/periods/{id}/reminders", periodId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipientCount").value(2));
        mockMvc.perform(auth.as(f.tdp1Secretary(), post("/api/fees/periods/{id}/reminders", periodId)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("FEE_REMINDER_TOO_SOON"));

        assertThat(feeReminders(f.tdp1Deputy())).containsExactly(periodId);
        assertThat(feeReminders(f.tdp1Member())).isEmpty();
    }

    @Test
    void closedPeriodTakesNoPayments() throws Exception {
        String periodId = createPeriod(f.wardSecretary(), f.ward().getId(), null);
        Map<String, Object> closed = periodPayload(f.ward().getId(), null);
        closed.put("status", "CLOSED");
        mockMvc.perform(auth.as(f.wardSecretary(), put("/api/fees/periods/{id}", periodId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(closed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        mockMvc.perform(auth.as(f.wardSecretary(), post("/api/fees/periods/{id}/payments", periodId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("memberId", f.tdp1Member().getId(), "method", "CASH"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FEE_PERIOD_CLOSED"));
    }

    private List<String> feeReminders(Member member) {
        return jdbcTemplate.queryForList(
                "select n.reference_id from notifications n join user_notifications un on un.notification_id = n.id"
                        + " where un.member_id = ? and n.notification_type = 'FEE_REMINDER'",
                String.class, member.getId());
    }

    private String createPeriod(Member creator, String organizationId, String collectorId) throws Exception {
        var result = mockMvc.perform(auth.as(creator, post("/api/fees/periods"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(periodPayload(organizationId, collectorId))))
                .andExpect(status().isCreated())
                .andReturn();
        return body(result).get("id").asText();
    }

    private void pay(Member officer, String periodId, Member member) throws Exception {
        mockMvc.perform(auth.as(officer, post("/api/fees/periods/{id}/payments", periodId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("memberId", member.getId(), "method", "TRANSFER"))))
                .andExpect(status().isCreated());
    }

    private Map<String, Object> periodPayload(String organizationId, String collectorId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", "Doan phi nam 2026");
        payload.put("organizationId", organizationId);
        payload.put("amount", 60000);
        payload.put("dueDate", "2026-12-31");
        payload.put("collectorMemberId", collectorId);
        return payload;
    }
}
