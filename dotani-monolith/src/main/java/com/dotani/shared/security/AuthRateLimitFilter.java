package com.dotani.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Per-client-IP rate limits on the public auth endpoints (brute force, sign-up spam, reset-mail spam).
 * Buckets live in memory: fine for one instance; several instances would need a shared store (Redis).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private record Limit(int capacity, Duration period) {
    }

    private static final Map<String, Limit> LIMITS = Map.of(
            "/api/auth/login", new Limit(10, Duration.ofMinutes(1)),
            "/api/auth/google", new Limit(10, Duration.ofMinutes(1)),
            "/api/auth/register", new Limit(5, Duration.ofMinutes(10)),
            "/api/auth/refresh", new Limit(30, Duration.ofMinutes(1)),
            "/api/auth/password/forgot", new Limit(5, Duration.ofMinutes(15)),
            "/api/auth/password/reset", new Limit(10, Duration.ofMinutes(15))
    );

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final boolean trustForwardedFor;

    public AuthRateLimitFilter(
            ObjectMapper objectMapper,
            @Value("${app.rate-limit.enabled:true}") boolean enabled,
            // Render and other reverse proxies put the client address in X-Forwarded-For
            @Value("${app.rate-limit.trust-forwarded-for:true}") boolean trustForwardedFor
    ) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.trustForwardedFor = trustForwardedFor;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || !"POST".equals(request.getMethod()) || !LIMITS.containsKey(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        Limit limit = LIMITS.get(path);
        Bucket bucket = buckets.computeIfAbsent(path + "|" + clientIp(request), key -> Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(limit.capacity()).refillGreedy(limit.capacity(), limit.period()).build())
                .build());

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        body.put("code", "RATE_LIMITED");
        body.put("message", "Bạn thao tác quá nhanh. Vui lòng thử lại sau " + retryAfterSeconds + " giây.");
        body.put("path", path);
        body.put("details", List.of());
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    /** Clears all counters (buckets refill on their own; this only bounds memory). Also used by tests. */
    @Scheduled(fixedDelayString = "PT1H")
    public void reset() {
        buckets.clear();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = trustForwardedFor ? request.getHeader("X-Forwarded-For") : null;
        return forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }
}
