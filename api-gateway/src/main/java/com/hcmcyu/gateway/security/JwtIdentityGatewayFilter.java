package com.hcmcyu.gateway.security;

import java.util.List;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtIdentityGatewayFilter implements GlobalFilter, Ordered {

    private static final List<String> IDENTITY_HEADERS = List.of(
            "X-User-Id",
            "X-Member-Id",
            "X-Username",
            "X-User-Email",
            "X-User-Role",
            "X-Organization-Id",
            "X-Tdp-Id"
    );

    private final GatewayJwtService gatewayJwtService;

    public JwtIdentityGatewayFilter(GatewayJwtService gatewayJwtService) {
        this.gatewayJwtService = gatewayJwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest sanitizedRequest = removeIdentityHeaders(exchange.getRequest());
        ServerWebExchange sanitizedExchange = exchange.mutate().request(sanitizedRequest).build();

        if (isPublicPath(sanitizedRequest.getURI().getRawPath())) {
            return chain.filter(sanitizedExchange);
        }

        String authorization = sanitizedRequest.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED"));
        }

        GatewayPrincipal principal = gatewayJwtService.parseAccessToken(authorization.substring(7));
        ServerHttpRequest authenticatedRequest = sanitizedRequest.mutate()
                .headers(headers -> applyPrincipal(headers, principal))
                .build();
        return chain.filter(sanitizedExchange.mutate().request(authenticatedRequest).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    private ServerHttpRequest removeIdentityHeaders(ServerHttpRequest request) {
        return request.mutate()
                .headers(headers -> IDENTITY_HEADERS.forEach(headers::remove))
                .build();
    }

    private void applyPrincipal(HttpHeaders headers, GatewayPrincipal principal) {
        putIfPresent(headers, "X-User-Id", principal.userId());
        putIfPresent(headers, "X-Member-Id", principal.memberId());
        putIfPresent(headers, "X-Username", principal.username());
        putIfPresent(headers, "X-User-Email", principal.email());
        putIfPresent(headers, "X-User-Role", principal.role());
        putIfPresent(headers, "X-Organization-Id", principal.organizationId());
        putIfPresent(headers, "X-Tdp-Id", principal.tdpId());
    }

    private void putIfPresent(HttpHeaders headers, String name, String value) {
        if (value != null && !value.isBlank()) {
            headers.set(name, value);
        }
    }

    private boolean isPublicPath(String path) {
        return path.endsWith("/health")
                || path.equals("/actuator/health")
                || path.startsWith("/actuator/health/")
                || path.startsWith("/v3/api-docs")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/webjars/swagger-ui")
                || path.equals("/api/auth/register")
                || path.equals("/api/auth/login")
                || path.equals("/api/auth/refresh")
                || path.equals("/api/organizations/public")
                || path.startsWith("/uploads/avatars/")
                || path.startsWith("/ws/chat");
    }
}
