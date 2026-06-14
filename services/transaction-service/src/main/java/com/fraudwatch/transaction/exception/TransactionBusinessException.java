package com.fraudwatch.transaction.exception;

import org.springframework.http.HttpStatus;

public class TransactionBusinessException extends RuntimeException {

    private final HttpStatus status;

    public TransactionBusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

