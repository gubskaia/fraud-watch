package com.fraudwatch.review.exception;

import org.springframework.http.HttpStatus;

public class ReviewBusinessException extends RuntimeException {

    private final HttpStatus status;

    public ReviewBusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

