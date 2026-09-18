package com.hcmcyu.shared.exception;

import com.hcmcyu.auth.exception.ApiErrorResponse;
import com.hcmcyu.auth.exception.AuthException;
import com.hcmcyu.audit.exception.AuditServiceException;
import com.hcmcyu.chat.exception.ChatServiceException;
import com.hcmcyu.content.exception.ContentServiceException;
import com.hcmcyu.event.exception.EventServiceException;
import com.hcmcyu.member.exception.MemberServiceException;
import com.hcmcyu.notification.exception.NotificationServiceException;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class MonolithExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(MonolithExceptionHandler.class);

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiErrorResponse> handle(AuthException exception, WebRequest request) {
        return error(exception.getStatus(), exception.getCode(), exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(MemberServiceException.class)
    public ResponseEntity<ApiErrorResponse> handle(MemberServiceException exception, WebRequest request) {
        return error(exception.getStatus(), exception.getCode(), exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(EventServiceException.class)
    public ResponseEntity<ApiErrorResponse> handle(EventServiceException exception, WebRequest request) {
        return error(exception.getStatus(), exception.getCode(), exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(ContentServiceException.class)
    public ResponseEntity<ApiErrorResponse> handle(ContentServiceException exception, WebRequest request) {
        return error(exception.getStatus(), exception.getCode(), exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(ChatServiceException.class)
    public ResponseEntity<ApiErrorResponse> handle(ChatServiceException exception, WebRequest request) {
        return error(exception.getStatus(), exception.getCode(), exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(NotificationServiceException.class)
    public ResponseEntity<ApiErrorResponse> handle(NotificationServiceException exception, WebRequest request) {
        return error(exception.getStatus(), exception.getCode(), exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(AuditServiceException.class)
    public ResponseEntity<ApiErrorResponse> handle(AuditServiceException exception, WebRequest request) {
        return error(exception.getStatus(), exception.getCode(), exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            WebRequest request
    ) {
        List<String> details = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .toList();
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", request, details);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUpload(WebRequest request) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "Uploaded file is too large", request, List.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableBody(
            HttpMessageNotReadableException exception,
            WebRequest request
    ) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST_BODY", "Request body is invalid or malformed", request, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException exception, WebRequest request) {
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(Exception exception, WebRequest request) {
        log.error("Unhandled backend exception at {}", path(request), exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Internal server error", request, List.of());
    }

    private ResponseEntity<ApiErrorResponse> error(
            HttpStatus status,
            String code,
            String message,
            WebRequest request,
            List<String> details
    ) {
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(
                        OffsetDateTime.now(),
                        status.value(),
                        code,
                        message,
                        path(request),
                        details
                ));
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private String path(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }
        return "";
    }
}
