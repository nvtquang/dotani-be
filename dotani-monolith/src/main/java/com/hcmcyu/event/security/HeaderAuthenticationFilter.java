package com.hcmcyu.event.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String roleHeader = request.getHeader("X-User-Role");
        String userId = request.getHeader("X-User-Id");

        if (roleHeader == null || userId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Role role = Role.valueOf(roleHeader);
            CurrentUser currentUser = new CurrentUser(
                    userId,
                    request.getHeader("X-Member-Id"),
                    request.getHeader("X-Username"),
                    role,
                    request.getHeader("X-Organization-Id"),
                    request.getHeader("X-Tdp-Id")
            );
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    currentUser,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
