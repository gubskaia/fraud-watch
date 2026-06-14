package com.fraudwatch.fraud.exception;

import org.springframework.http.HttpStatus;

public class FraudBusinessException extends RuntimeException {

    private final HttpStatus status;

    public FraudBusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

