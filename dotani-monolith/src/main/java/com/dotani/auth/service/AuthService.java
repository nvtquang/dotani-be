package com.dotani.auth.service;

import com.dotani.auth.dto.AuthResponse;
import com.dotani.auth.dto.ChangePasswordRequest;
import com.dotani.auth.dto.ForgotPasswordRequest;
import com.dotani.auth.dto.LogoutRequest;
import com.dotani.auth.dto.ResetPasswordRequest;
import com.dotani.auth.entity.PasswordResetToken;
import com.dotani.auth.repository.PasswordResetTokenRepository;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import com.dotani.auth.dto.GoogleLoginRequest;
import com.dotani.auth.dto.GoogleUserInfo;
import com.dotani.auth.dto.LoginRequest;
import com.dotani.auth.dto.RefreshRequest;
import com.dotani.auth.dto.RegisterRequest;
import com.dotani.auth.dto.UserResponse;
import com.dotani.auth.entity.AuthProvider;
import com.dotani.auth.entity.UserAccount;
import com.dotani.shared.exception.ApiException;
import com.dotani.auth.mapper.UserMapper;
import com.dotani.auth.repository.UserAccountRepository;
import com.dotani.member.service.MemberAccessService;
import com.dotani.member.service.MemberRegistrationService;
import com.dotani.shared.security.AccessScope;
import com.dotani.member.service.MemberRegistrationService.RegisteredMember;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;
    private final MemberRegistrationService memberRegistrationService;
    private final MemberAccessService memberAccessService;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final LoginAttemptService loginAttemptService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordResetMailer passwordResetMailer;
    private final String frontendUrl;
    private final Duration resetLinkValidity;

    public AuthService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            UserMapper userMapper,
            MemberRegistrationService memberRegistrationService,
            MemberAccessService memberAccessService,
            GoogleTokenVerifier googleTokenVerifier,
            LoginAttemptService loginAttemptService,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordResetMailer passwordResetMailer,
            @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl,
            @Value("${app.security.password-reset.valid-minutes:30}") long resetLinkValidMinutes
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userMapper = userMapper;
        this.memberRegistrationService = memberRegistrationService;
        this.memberAccessService = memberAccessService;
        this.googleTokenVerifier = googleTokenVerifier;
        this.loginAttemptService = loginAttemptService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordResetMailer = passwordResetMailer;
        this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        this.resetLinkValidity = Duration.ofMinutes(resetLinkValidMinutes);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();

        PasswordPolicy.requireStrong(request.password());
        if (userAccountRepository.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "DUPLICATE_USERNAME", "Username already exists");
        }
        if (userAccountRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "Email already exists");
        }

        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEnabled(true);

        UserAccount savedUser = userAccountRepository.saveAndFlush(user);
        RegisteredMember member = memberRegistrationService.register(
                savedUser.getId(),
                request.fullName(),
                request.phone(),
                request.dateOfBirth(),
                request.organizationId()
        );
        linkMember(savedUser, member);
        return buildAuthResponse(savedUser, refreshTokenService.createRefreshToken(savedUser));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String usernameOrEmail = request.usernameOrEmail().trim();
        UserAccount user = userAccountRepository.findByUsername(usernameOrEmail)
                .or(() -> userAccountRepository.findByEmail(usernameOrEmail.toLowerCase()))
                .orElse(null);
        if (user != null) {
            loginAttemptService.requireNotLocked(user);
        }
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    usernameOrEmail,
                    request.password()
            ));
        } catch (BadCredentialsException exception) {
            if (user != null) {
                loginAttemptService.recordFailure(user.getId());
            }
            throw invalidCredentials();
        }
        if (user == null) {
            throw invalidCredentials();
        }
        loginAttemptService.recordSuccess(user.getId());
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);

        return buildAuthResponse(user, refreshTokenService.createRefreshToken(user));
    }

    /** Revokes the given refresh token, or every refresh token of its owner when allDevices is true. */
    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenService.revoke(request.refreshToken()).ifPresent(user -> {
            if (Boolean.TRUE.equals(request.allDevices())) {
                refreshTokenService.revokeAllForUser(user.getId());
            }
        });
    }

    /** Changes the password, signs out other sessions and returns fresh tokens for this one. */
    @Transactional
    public AuthResponse changePassword(String userId, ChangePasswordRequest request) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CURRENT_PASSWORD", "Current password is incorrect");
        }
        PasswordPolicy.requireStrong(request.newPassword());
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokenService.revokeAllForUser(user.getId());
        return buildAuthResponse(user, refreshTokenService.createRefreshToken(user));
    }

    /**
     * Emails a reset link if the address belongs to an account. Always succeeds silently so the
     * endpoint cannot be used to find out which emails are registered.
     */
    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        userAccountRepository.findByEmail(request.email().trim().toLowerCase()).ifPresent(user -> {
            passwordResetTokenRepository.invalidateAllForUser(user.getId());
            String rawToken = refreshTokenService.newOpaqueToken();
            passwordResetTokenRepository.save(new PasswordResetToken(
                    user,
                    refreshTokenService.hash(rawToken),
                    Instant.now().plus(resetLinkValidity)
            ));
            passwordResetMailer.send(
                    user.getEmail(),
                    frontendUrl + "/reset-password?token=" + rawToken,
                    resetLinkValidity.toMinutes()
            );
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(refreshTokenService.hash(request.token()))
                .filter(token -> token.isUsable(Instant.now()))
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_RESET_TOKEN",
                        "Password reset link is invalid or has expired"
                ));
        PasswordPolicy.requireStrong(request.newPassword());
        UserAccount user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        resetToken.markUsed(Instant.now());
        passwordResetTokenRepository.invalidateAllForUser(user.getId());
        refreshTokenService.revokeAllForUser(user.getId());
    }

    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleUserInfo googleUser = googleTokenVerifier.verify(request.idToken());
        String email = googleUser.email().trim().toLowerCase();

        UserAccount user = userAccountRepository
                .findByAuthProviderAndProviderSubject(AuthProvider.GOOGLE, googleUser.subject())
                .or(() -> userAccountRepository.findByEmail(email))
                .orElseGet(() -> createGoogleUser(email, googleUser));

        if (user.getAuthProvider() == AuthProvider.LOCAL) {
            user.setAuthProvider(AuthProvider.GOOGLE);
            user.setProviderSubject(googleUser.subject());
        }
        if (user.getProviderSubject() == null || user.getProviderSubject().isBlank()) {
            user.setProviderSubject(googleUser.subject());
        }
        user.setEnabled(true);

        UserAccount savedUser = userAccountRepository.saveAndFlush(user);
        if (savedUser.getMemberId() == null || savedUser.getMemberId().isBlank()) {
            RegisteredMember member = memberRegistrationService.register(
                    savedUser.getId(),
                    request.fullName(),
                        request.phone(),
                    request.dateOfBirth(),
                    request.organizationId()
            );
            linkMember(savedUser, member);
        } else {
            memberRegistrationService.completeProfile(
                    savedUser.getMemberId(),
                    request.fullName(),
                    request.phone(),
                    request.dateOfBirth()
            );
        }

        return buildAuthResponse(savedUser, refreshTokenService.createRefreshToken(savedUser));
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        UserAccount user = refreshTokenService.consumeRefreshToken(request.refreshToken());
        return buildAuthResponse(user, refreshTokenService.createRefreshToken(user));
    }

    public UserResponse me(String userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        return userMapper.toResponse(user, memberAccessService.scopeOf(user.getMemberId()));
    }

    private AuthResponse buildAuthResponse(UserAccount user, String refreshToken) {
        AccessScope scope = memberAccessService.scopeOf(user.getMemberId());
        return new AuthResponse(
                jwtService.createAccessToken(user, scope),
                refreshToken,
                "Bearer",
                jwtService.getAccessTokenExpirationMs(),
                userMapper.toResponse(user, scope)
        );
    }

    private UserAccount createGoogleUser(String email, GoogleUserInfo googleUser) {
        UserAccount user = new UserAccount();
        user.setUsername(generateGoogleUsername(email));
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
        user.setAuthProvider(AuthProvider.GOOGLE);
        user.setProviderSubject(googleUser.subject());
        user.setEnabled(true);
        return user;
    }

    private String generateGoogleUsername(String email) {
        String prefix = email.substring(0, email.indexOf('@'))
                .replaceAll("[^a-zA-Z0-9._-]", ".")
                .replaceAll("\\.+", ".")
                .replaceAll("^\\.|\\.$", "");
        if (prefix.length() < 3) {
            prefix = "google.user";
        }
        String candidate = prefix;
        int suffix = 1;
        while (userAccountRepository.existsByUsername(candidate)) {
            candidate = prefix + "." + suffix;
            suffix++;
        }
        return candidate;
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid username/email or password");
    }
    private void linkMember(UserAccount user, RegisteredMember member) {
        user.setMemberId(member.memberId());
    }
}
