package com.hcmcyu.auth.service;

import com.hcmcyu.auth.dto.AuthResponse;
import com.hcmcyu.auth.dto.GoogleLoginRequest;
import com.hcmcyu.auth.dto.GoogleUserInfo;
import com.hcmcyu.auth.dto.LoginRequest;
import com.hcmcyu.auth.dto.RefreshRequest;
import com.hcmcyu.auth.dto.RegisterRequest;
import com.hcmcyu.auth.dto.UserResponse;
import com.hcmcyu.auth.entity.AuthProvider;
import com.hcmcyu.auth.entity.Role;
import com.hcmcyu.auth.entity.UserAccount;
import com.hcmcyu.auth.exception.AuthException;
import com.hcmcyu.auth.mapper.UserMapper;
import com.hcmcyu.auth.repository.UserAccountRepository;
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
    private final MemberRegistrationClient memberRegistrationClient;
    private final GoogleTokenVerifier googleTokenVerifier;

    public AuthService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            UserMapper userMapper,
            MemberRegistrationClient memberRegistrationClient,
            GoogleTokenVerifier googleTokenVerifier
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userMapper = userMapper;
        this.memberRegistrationClient = memberRegistrationClient;
        this.googleTokenVerifier = googleTokenVerifier;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();

        if (userAccountRepository.existsByUsername(username)) {
            throw new AuthException(HttpStatus.CONFLICT, "DUPLICATE_USERNAME", "Username already exists");
        }
        if (userAccountRepository.existsByEmail(email)) {
            throw new AuthException(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "Email already exists");
        }

        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.MEMBER);
        user.setEnabled(true);

        UserAccount savedUser = userAccountRepository.saveAndFlush(user);
        var memberProfile = memberRegistrationClient.createMemberProfile(savedUser, request);
        if (memberProfile == null || memberProfile.id() == null || memberProfile.organizationId() == null) {
            throw new AuthException(
                    HttpStatus.BAD_GATEWAY,
                    "MEMBER_PROFILE_REGISTRATION_FAILED",
                    "Could not create member profile"
            );
        }
        savedUser.setMemberId(memberProfile.id());
        savedUser.setOrganizationId(memberProfile.organizationId());
        savedUser.setTdpId(memberProfile.organizationId());
        return buildAuthResponse(savedUser, refreshTokenService.createRefreshToken(savedUser));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String usernameOrEmail = request.usernameOrEmail().trim();
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    usernameOrEmail,
                    request.password()
            ));
        } catch (BadCredentialsException exception) {
            throw invalidCredentials();
        }

        UserAccount user = userAccountRepository.findByUsername(usernameOrEmail)
                .or(() -> userAccountRepository.findByEmail(usernameOrEmail.toLowerCase()))
                .orElseThrow(this::invalidCredentials);

        return buildAuthResponse(user, refreshTokenService.createRefreshToken(user));
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
        if (user.getRole() == null) {
            user.setRole(Role.MEMBER);
        }
        user.setEnabled(true);

        UserAccount savedUser = userAccountRepository.saveAndFlush(user);
        if (savedUser.getMemberId() == null || savedUser.getMemberId().isBlank()) {
            var memberProfile = memberRegistrationClient.createMemberProfile(savedUser, request);
            if (memberProfile == null || memberProfile.id() == null || memberProfile.organizationId() == null) {
                throw new AuthException(
                        HttpStatus.BAD_GATEWAY,
                        "MEMBER_PROFILE_REGISTRATION_FAILED",
                        "Could not create member profile"
                );
            }
            savedUser.setMemberId(memberProfile.id());
            savedUser.setOrganizationId(memberProfile.organizationId());
            savedUser.setTdpId(memberProfile.organizationId());
        } else {
            memberRegistrationClient.completeMemberProfile(savedUser, request);
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
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        return userMapper.toResponse(user);
    }

    private AuthResponse buildAuthResponse(UserAccount user, String refreshToken) {
        return new AuthResponse(
                jwtService.createAccessToken(user),
                refreshToken,
                "Bearer",
                jwtService.getAccessTokenExpirationMs(),
                userMapper.toResponse(user)
        );
    }

    private UserAccount createGoogleUser(String email, GoogleUserInfo googleUser) {
        UserAccount user = new UserAccount();
        user.setUsername(generateGoogleUsername(email));
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
        user.setAuthProvider(AuthProvider.GOOGLE);
        user.setProviderSubject(googleUser.subject());
        user.setRole(Role.MEMBER);
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

    private AuthException invalidCredentials() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid username/email or password");
    }
}
