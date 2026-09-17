package com.hcmcyu.shared.security;

import com.hcmcyu.auth.exception.AuthException;
import com.hcmcyu.auth.security.AuthPrincipal;
import com.hcmcyu.auth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class MonolithJwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public MonolithJwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            AuthPrincipal principal = jwtService.parseAccessToken(header.substring(7));
            Object domainPrincipal = resolvePrincipal(request.getRequestURI(), principal);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    domainPrincipal,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (AuthException exception) {
            SecurityContextHolder.clearContext();
            request.setAttribute("authException", exception);
            filterChain.doFilter(request, response);
        }
    }

    private Object resolvePrincipal(String path, AuthPrincipal principal) {
        if (path.startsWith("/api/members")
                || path.startsWith("/api/organizations")
                || path.startsWith("/api/dashboard/member-summary")) {
            return new com.hcmcyu.member.security.CurrentUser(
                    principal.userId(),
                    principal.memberId(),
                    principal.username(),
                    com.hcmcyu.member.security.Role.valueOf(principal.role().name()),
                    principal.organizationId(),
                    principal.tdpId()
            );
        }
        if (path.startsWith("/api/events") || path.startsWith("/api/dashboard/event-summary")) {
            return new com.hcmcyu.event.security.CurrentUser(
                    principal.userId(),
                    principal.memberId(),
                    principal.username(),
                    com.hcmcyu.event.security.Role.valueOf(principal.role().name()),
                    principal.organizationId(),
                    principal.tdpId()
            );
        }
        if (path.startsWith("/api/posts") || path.startsWith("/api/dashboard/content-summary")) {
            return new com.hcmcyu.content.security.CurrentUser(
                    principal.userId(),
                    principal.memberId(),
                    principal.username(),
                    com.hcmcyu.content.security.Role.valueOf(principal.role().name()),
                    principal.organizationId(),
                    principal.tdpId()
            );
        }
        if (path.startsWith("/api/chat")) {
            return new com.hcmcyu.chat.security.CurrentUser(
                    principal.userId(),
                    principal.memberId(),
                    principal.username(),
                    principal.email(),
                    com.hcmcyu.chat.security.Role.valueOf(principal.role().name()),
                    principal.organizationId(),
                    principal.tdpId()
            );
        }
        if (path.startsWith("/api/notifications")) {
            return new com.hcmcyu.notification.security.CurrentUser(
                    principal.userId(),
                    principal.memberId(),
                    principal.username(),
                    principal.email(),
                    com.hcmcyu.notification.security.Role.valueOf(principal.role().name()),
                    principal.organizationId(),
                    principal.tdpId()
            );
        }
        if (path.startsWith("/api/audit-logs")) {
            return new com.hcmcyu.audit.security.CurrentUser(
                    principal.userId(),
                    principal.memberId(),
                    principal.username(),
                    com.hcmcyu.audit.security.Role.valueOf(principal.role().name()),
                    principal.organizationId(),
                    principal.tdpId()
            );
        }
        return principal;
    }
}
