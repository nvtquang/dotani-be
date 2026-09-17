package com.hcmcyu.audit.controller;

import com.hcmcyu.audit.dto.AuditLogCreateRequest;
import com.hcmcyu.audit.dto.AuditLogResponse;
import com.hcmcyu.audit.entity.AuditAction;
import com.hcmcyu.audit.entity.AuditResult;
import com.hcmcyu.audit.entity.ResourceType;
import com.hcmcyu.audit.security.CurrentUser;
import com.hcmcyu.audit.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Audit Logs", description = "Audit log APIs for administrative actions. Sensitive values are never stored.")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT Bearer token or internal secret"),
        @ApiResponse(responseCode = "403", description = "Role denied")
})
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @PostMapping("/internal/audit-logs")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create audit log internally", description = "Internal service-to-service endpoint protected by X-Internal-Secret.")
    public AuditLogResponse createInternal(
            @RequestHeader(name = "X-Internal-Secret", required = false) String internalSecret,
            @Valid @RequestBody AuditLogCreateRequest request
    ) {
        return auditLogService.createInternal(request, internalSecret);
    }

    @GetMapping("/api/audit-logs")
    @Operation(summary = "List audit logs", description = "WARD_SECRETARY can view all audit logs. WARD_DEPUTY_SECRETARY can view permitted business audit logs, excluding role-change audit details.", security = @SecurityRequirement(name = "bearerAuth"))
    public Page<AuditLogResponse> findAll(
            @RequestParam(required = false, name = "action") AuditAction action,
            @RequestParam(required = false, name = "resourceType") ResourceType resourceType,
            @RequestParam(required = false, name = "organizationId") String organizationId,
            @RequestParam(required = false, name = "result") AuditResult result,
            @RequestParam(required = false, name = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Pageable pageable,
            @AuthenticationPrincipal CurrentUser currentUser
    ) {
        return auditLogService.findAll(action, resourceType, organizationId, result, date, pageable, currentUser);
    }
}
