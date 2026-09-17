package com.hcmcyu.content.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "Standard API error response")
public record ApiErrorResponse(
        @Schema(description = "Time when the error response was created")
        OffsetDateTime timestamp,
        @Schema(description = "HTTP status code")
        int status,
        @Schema(description = "Stable application error code")
        String code,
        @Schema(description = "Human-readable error message")
        String message,
        @Schema(description = "Request path that produced the error")
        String path,
        @Schema(description = "Validation or business rule details")
        List<String> details
) {
}
