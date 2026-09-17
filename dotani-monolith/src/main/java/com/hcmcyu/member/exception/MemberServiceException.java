package com.hcmcyu.member.exception;

import org.springframework.http.HttpStatus;

public class MemberServiceException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public MemberServiceException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
