package com.dotani.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dotani.audit.entity.AuditAction;
import com.dotani.audit.entity.AuditLog;
import com.dotani.audit.repository.AuditLogRepository;
import com.dotani.member.entity.MemberRole;
import com.dotani.support.IntegrationTest;
import com.dotani.support.TestData.Fixture;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** Audit logs carry before/after values of changed fields, the client IP and the user agent. */
class AuditDetailsIntegrationTest extends IntegrationTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void roleChangeRecordsBeforeAfterIpAndUserAgent() throws Exception {
        Fixture f = data.standardFixture();

        mockMvc.perform(auth.as(f.wardSecretary(), put("/api/members/{id}/role", f.tdp1Member().getId()))
                        .header(HttpHeaders.USER_AGENT, "JUnit-Browser/1.0")
                        .header("X-Forwarded-For", "203.0.113.7, 10.0.0.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("role", MemberRole.TDP_DEPUTY_SECRETARY.name()))))
                .andExpect(status().isOk());

        AuditLog log = onlyLog(AuditAction.CHANGE_ROLE);
        assertThat(log.getDetails()).isEqualTo(Map.of(
                "memberRole", Map.of("before", "MEMBER", "after", "TDP_DEPUTY_SECRETARY")));
        assertThat(log.getIp()).isEqualTo("203.0.113.7");
        assertThat(log.getUserAgent()).isEqualTo("JUnit-Browser/1.0");
    }

    @Test
    void memberUpdateRecordsOnlyChangedFieldsAndNoContactData() throws Exception {
        Fixture f = data.standardFixture();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", f.tdp1Member().getUserId());
        payload.put("fullName", "Ten moi");
        payload.put("phone", "0912345678");
        payload.put("organizationId", f.tdp1().getId());

        mockMvc.perform(auth.as(f.wardSecretary(), put("/api/members/{id}", f.tdp1Member().getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(payload)))
                .andExpect(status().isOk());

        AuditLog log = onlyLog(AuditAction.UPDATE_MEMBER);
        assertThat(log.getDetails()).containsOnlyKeys("fullName");
        assertThat(log.getDetails().get("fullName")).isEqualTo(Map.of("before", "Doan vien TDP 1", "after", "Ten moi"));
        assertThat(log.getDetails().toString()).doesNotContain("0912345678");
    }

    private AuditLog onlyLog(AuditAction action) {
        return auditLogRepository.findAll().stream()
                .filter(log -> log.getAction() == action)
                .reduce((first, second) -> {
                    throw new AssertionError("more than one " + action + " log");
                })
                .orElseThrow(() -> new AssertionError("no " + action + " log"));
    }
}
