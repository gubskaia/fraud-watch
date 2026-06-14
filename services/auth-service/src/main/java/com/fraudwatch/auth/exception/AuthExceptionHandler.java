package com.fraudwatch.auth.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {

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

    @ExceptionHandler(AuthBusinessException.class)
    ResponseEntity<Map<String, Object>> handleBusiness(
        AuthBusinessException exception,
        HttpServletRequest request
    ) {
        return ResponseEntity.status(exception.getStatus()).body(errorBody(
            exception.getStatus(),
            exception.getMessage(),
            request.getRequestURI(),
            Map.of()
        ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Map<String, Object>> handleAccessDenied(
        AccessDeniedException exception,
        HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody(
            HttpStatus.FORBIDDEN,
            "Access denied",
            request.getRequestURI(),
            Map.of()
        ));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> handleUnhandled(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal server error",
            request.getRequestURI(),
            Map.of("error", exception.getClass().getSimpleName())
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
