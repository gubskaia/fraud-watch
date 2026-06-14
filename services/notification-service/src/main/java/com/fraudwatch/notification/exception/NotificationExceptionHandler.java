package com.fraudwatch.notification.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class NotificationExceptionHandler {

    @ExceptionHandler(NotificationBusinessException.class)
    ResponseEntity<Map<String, Object>> handleBusiness(
        NotificationBusinessException exception,
        HttpServletRequest request
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", exception.getStatus().value());
        body.put("error", exception.getStatus().getReasonPhrase());
        body.put("message", exception.getMessage());
        body.put("path", request.getRequestURI());
        return ResponseEntity.status(exception.getStatus()).body(body);
    }
}
