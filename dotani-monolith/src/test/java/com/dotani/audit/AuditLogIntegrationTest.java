package com.dotani.audit;

import com.dotani.support.IntegrationTest;
import com.dotani.audit.entity.AuditAction;
import com.dotani.audit.entity.ResourceType;
import com.dotani.audit.service.AuditLogService;
import com.dotani.shared.security.CurrentUser;
import com.dotani.shared.security.Role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.dotani.audit.entity.AuditLog;
import com.dotani.audit.entity.AuditResult;
import com.dotani.audit.repository.AuditLogRepository;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class AuditLogIntegrationTest extends IntegrationTest {


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
    }

    @Test
    void recordedAuditLogIsListedWithoutSensitivePayload() throws Exception {
        auditLogService.record(
                new CurrentUser("ward-secretary-user", "ward-secretary-member", "ward.secretary", null,
                        Role.WARD_SECRETARY, "ward-thuong-cat", null),
                AuditAction.CHANGE_ROLE,
                ResourceType.MEMBER,
                "member-1",
                "tdp-1"
        );

        assertThat(auditLogRepository.findAll()).singleElement().satisfies(log -> {
            assertThat(log.getAction()).isEqualTo(AuditAction.CHANGE_ROLE);
            assertThat(log.getResourceType()).isEqualTo(ResourceType.MEMBER);
            assertThat(log.getActorUserId()).isEqualTo("ward-secretary-user");
        });
        mockMvc.perform(withWardSecretary(get("/api/audit-logs")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("CHANGE_ROLE"))
                .andExpect(jsonPath("$.content[0].resourceType").value("MEMBER"));
    }

    @Test
    void wardSecretaryCanViewAllAuditLogsWithFilters() throws Exception {
        saveAuditLog(AuditAction.CHANGE_ROLE, ResourceType.MEMBER, "member-1", "tdp-1");
        saveAuditLog(AuditAction.CREATE_POST, ResourceType.POST, "post-1", "tdp-2");

        mockMvc.perform(withWardSecretary(get("/api/audit-logs?action=CHANGE_ROLE&page=0&size=10")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].action").value("CHANGE_ROLE"));
    }

    @Test
    void wardDeputySecretaryCanViewBusinessAuditButNotRoleChanges() throws Exception {
        saveAuditLog(AuditAction.CHANGE_ROLE, ResourceType.MEMBER, "member-1", "tdp-1");
        saveAuditLog(AuditAction.UPDATE_EVENT, ResourceType.EVENT, "event-1", "tdp-1");

        mockMvc.perform(withWardDeputySecretary(get("/api/audit-logs?page=0&size=10")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].action").value("UPDATE_EVENT"));
    }

    @Test
    void tdpSecretaryCannotViewAuditLogs() throws Exception {
        saveAuditLog(AuditAction.UPDATE_EVENT, ResourceType.EVENT, "event-1", "tdp-1");

        mockMvc.perform(withTdpSecretary(get("/api/audit-logs")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUDIT_LOG_FORBIDDEN"));
    }

    private void saveAuditLog(
            AuditAction action,
            ResourceType resourceType,
            String resourceId,
            String organizationId
    ) {
        AuditLog auditLog = new AuditLog();
        auditLog.setActorUserId("actor-user");
        auditLog.setActorRole("WARD_SECRETARY");
        auditLog.setAction(action);
        auditLog.setResourceType(resourceType);
        auditLog.setResourceId(resourceId);
        auditLog.setOrganizationId(organizationId);
        auditLog.setResult(AuditResult.SUCCESS);
        auditLogRepository.save(auditLog);
    }

    private MockHttpServletRequestBuilder withWardSecretary(MockHttpServletRequestBuilder request) {
        return request
                .with(auth.identity("ward-secretary-user", "WARD_SECRETARY", "ward-thuong-cat", null, null));
    }

    private MockHttpServletRequestBuilder withWardDeputySecretary(MockHttpServletRequestBuilder request) {
        return request
                .with(auth.identity("ward-deputy-user", "WARD_DEPUTY_SECRETARY", "ward-thuong-cat", null, null));
    }

    private MockHttpServletRequestBuilder withTdpSecretary(MockHttpServletRequestBuilder request) {
        return request
                .with(auth.identity("tdp-secretary-user", "TDP_SECRETARY", "ward-thuong-cat", "tdp-1", null));
    }

}
