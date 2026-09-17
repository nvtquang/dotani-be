package com.hcmcyu.auth.controller;

import com.hcmcyu.auth.dto.InternalRoleUpdateRequest;
import com.hcmcyu.auth.dto.UserResponse;
import com.hcmcyu.auth.service.InternalUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/users")
@Tag(name = "Internal Users", description = "Internal service-to-service user account APIs. Protected by X-Internal-Secret, not by frontend JWT.")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "401", description = "Missing or invalid internal secret"),
        @ApiResponse(responseCode = "404", description = "User account not found")
})
public class InternalUserController {

    private final InternalUserService internalUserService;

    public InternalUserController(InternalUserService internalUserService) {
        this.internalUserService = internalUserService;
    }

    @PutMapping("/{userId}/role")
    @Operation(summary = "Update account role internally", description = "Called by member-service after WARD_SECRETARY role assignment is authorized and audited.")
    public UserResponse updateRole(
            @PathVariable("userId") String userId,
            @Valid @RequestBody InternalRoleUpdateRequest request,
            @RequestHeader(name = "X-Internal-Secret", required = false) String internalSecret
    ) {
        return internalUserService.updateRole(userId, request, internalSecret);
    }
}
