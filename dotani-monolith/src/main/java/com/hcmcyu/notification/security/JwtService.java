package com.hcmcyu.notification.security;

import com.hcmcyu.notification.exception.NotificationServiceException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey secretKey;

    public JwtService(@Value("${auth.jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public CurrentUser parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return new CurrentUser(
                    claims.get("userId", String.class),
                    claims.get("memberId", String.class),
                    claims.get("username", String.class),
                    claims.get("email", String.class),
                    Role.valueOf(claims.get("role", String.class)),
                    claims.get("organizationId", String.class),
                    claims.get("tdpId", String.class)
            );
        } catch (ExpiredJwtException exception) {
            throw new NotificationServiceException(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "Access token has expired");
        } catch (IllegalArgumentException | JwtException | NullPointerException exception) {
            throw new NotificationServiceException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Access token is invalid");
        }
    }
}
