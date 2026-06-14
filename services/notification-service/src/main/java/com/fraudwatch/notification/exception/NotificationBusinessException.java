package com.fraudwatch.notification.exception;

import org.springframework.http.HttpStatus;

public class NotificationBusinessException extends RuntimeException {

    private final HttpStatus status;

    public NotificationBusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

