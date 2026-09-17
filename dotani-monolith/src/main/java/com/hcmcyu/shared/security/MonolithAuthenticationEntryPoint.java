package com.hcmcyu.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hcmcyu.auth.exception.ApiErrorResponse;
import com.hcmcyu.auth.exception.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class MonolithAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public MonolithAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        Object exception = request.getAttribute("authException");
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        String code = "UNAUTHORIZED";
        String message = "Authentication is required";

        if (exception instanceof AuthException auth) {
            status = auth.getStatus();
            code = auth.getCode();
            message = auth.getMessage();
        }

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), new ApiErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                code,
                message,
                request.getRequestURI(),
                List.of()
        ));
    }
}
