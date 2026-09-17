package com.hcmcyu.auth.service;

import com.hcmcyu.auth.entity.Role;
import com.hcmcyu.auth.entity.UserAccount;
import com.hcmcyu.auth.exception.AuthException;
import com.hcmcyu.auth.security.AuthPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final Duration accessTokenExpiration;

    public JwtService(
            @Value("${auth.jwt.secret}") String secret,
            @Value("${auth.jwt.access-token-expiration-ms}") long accessTokenExpirationMs
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = Duration.ofMillis(accessTokenExpirationMs);
    }

    public String createAccessToken(UserAccount user) {
        Instant now = Instant.now();
        return createAccessToken(user, now, now.plus(accessTokenExpiration));
    }

    public String createAccessToken(UserAccount user, Instant issuedAt, Instant expiresAt) {
        return Jwts.builder()
                .subject(user.getId())
                .claim("userId", user.getId())
                .claim("memberId", user.getMemberId())
                .claim("username", user.getUsername())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("organizationId", user.getOrganizationId())
                .claim("tdpId", user.getTdpId())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    public AuthPrincipal parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return new AuthPrincipal(
                    claims.get("userId", String.class),
                    claims.get("memberId", String.class),
                    claims.get("username", String.class),
                    claims.get("email", String.class),
                    Role.valueOf(claims.get("role", String.class)),
                    claims.get("organizationId", String.class),
                    claims.get("tdpId", String.class)
            );
        } catch (ExpiredJwtException exception) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "Access token has expired");
        } catch (IllegalArgumentException | JwtException exception) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Access token is invalid");
        }
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpiration.toMillis();
    }
}

