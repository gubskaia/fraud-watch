package com.fraudwatch.fraud.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class FraudExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> handleValidation(
        MethodArgumentNotValidException exception,
        HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                fieldError -> fieldError.getDefaultMessage() == null ? "Invalid value" : fieldError.getDefaultMessage(),
                (left, right) -> right,
                LinkedHashMap::new
            ));

        return ResponseEntity.badRequest().body(errorBody(
            HttpStatus.BAD_REQUEST,
            "Validation failed",
            request.getRequestURI(),
            Map.of("fields", fieldErrors)
        ));
    }

    @ExceptionHandler(FraudBusinessException.class)
    ResponseEntity<Map<String, Object>> handleBusiness(
        FraudBusinessException exception,
        HttpServletRequest request
    ) {
        return ResponseEntity.status(exception.getStatus()).body(errorBody(
            exception.getStatus(),
            exception.getMessage(),
            request.getRequestURI(),
            Map.of()
        ));
    }

    private Map<String, Object> errorBody(
        HttpStatus status,
        String message,
        String path,
        Map<String, ?> details
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", path);
        if (!details.isEmpty()) {
            body.put("details", details);
        }
        return body;
    }
}

