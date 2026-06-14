package com.fraudwatch.auth.exception;

import org.springframework.http.HttpStatus;

public class AuthBusinessException extends RuntimeException {

    private final HttpStatus status;

    public AuthBusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

