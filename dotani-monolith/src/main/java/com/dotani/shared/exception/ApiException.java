package com.dotani.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Business error with an HTTP status and a stable error code, rendered as {@link ApiErrorResponse}.
 * The client-facing message is the Vietnamese text for the code in messages.properties (with {@code args}
 * filled in); the English message here is the fallback and what ends up in logs.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final Object[] args;

    public ApiException(HttpStatus status, String code, String message) {
        this(status, code, message, new Object[0]);
    }

    public ApiException(HttpStatus status, String code, String message, Object... args) {
        super(message);
        this.status = status;
        this.code = code;
        this.args = args;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public Object[] getArgs() {
        return args.clone();
    }
}
