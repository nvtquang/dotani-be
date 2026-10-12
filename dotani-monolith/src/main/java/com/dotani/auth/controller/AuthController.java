package com.dotani.auth.controller;

import com.dotani.auth.dto.AuthResponse;
import com.dotani.auth.dto.ChangePasswordRequest;
import com.dotani.auth.dto.ForgotPasswordRequest;
import com.dotani.auth.dto.LogoutRequest;
import com.dotani.auth.dto.ResetPasswordRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import com.dotani.auth.dto.GoogleLoginRequest;
import com.dotani.auth.dto.LoginRequest;
import com.dotani.auth.dto.RefreshRequest;
import com.dotani.auth.dto.RegisterRequest;
import com.dotani.auth.dto.UserResponse;
import com.dotani.shared.security.CurrentUser;
import com.dotani.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public UserResponse me(@AuthenticationPrincipal CurrentUser principal) {
        return authService.me(principal.userId());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Log out", description = "Revokes the refresh token; with allDevices=true every session of the account is signed out.")
    public void logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
    }

    @PutMapping("/password")
    @Operation(summary = "Change password", description = "Requires the current password. Other sessions are signed out; fresh tokens are returned.", security = @SecurityRequirement(name = "bearerAuth"))
    public AuthResponse changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal CurrentUser principal
    ) {
        return authService.changePassword(principal.userId(), request);
    }

    @PostMapping("/password/forgot")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Request a password reset link", description = "Always 204, whether or not the email is registered.")
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestPasswordReset(request);
    }

    @PostMapping("/password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Reset password with a link token", description = "The token is single use and valid for 30 minutes; all sessions are signed out.")
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
    }
}
