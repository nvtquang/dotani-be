package com.hcmcyu.auth.controller;

import com.hcmcyu.auth.dto.AuthResponse;
import com.hcmcyu.auth.dto.GoogleLoginRequest;
import com.hcmcyu.auth.dto.LoginRequest;
import com.hcmcyu.auth.dto.RefreshRequest;
import com.hcmcyu.auth.dto.RegisterRequest;
import com.hcmcyu.auth.dto.UserResponse;
import com.hcmcyu.auth.security.AuthPrincipal;
import com.hcmcyu.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Registration, login, refresh token, and current authenticated user APIs.")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation error or malformed request"),
        @ApiResponse(responseCode = "401", description = "Authentication failed or token is invalid"),
        @ApiResponse(responseCode = "409", description = "Username or email already exists")
})
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register member account", description = "Public endpoint. Creates a MEMBER account only; clients cannot submit or assign role.")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Public endpoint. Returns accessToken and refreshToken when username/email and password are valid.")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/google")
    @Operation(summary = "Login with Google", description = "Public endpoint. Verifies Google ID token on the backend, creates or logs in a MEMBER account, and creates the member profile with required personal information.")
    public AuthResponse loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return authService.loginWithGoogle(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Public endpoint. Consumes a valid refresh token and rotates it.")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Requires JWT Bearer authentication.", security = @SecurityRequirement(name = "bearerAuth"))
    public UserResponse me(@AuthenticationPrincipal AuthPrincipal principal) {
        return authService.me(principal.userId());
    }
}
