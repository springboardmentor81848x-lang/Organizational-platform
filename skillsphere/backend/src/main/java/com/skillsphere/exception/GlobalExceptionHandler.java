package com.skillsphere.exception;

import com.skillsphere.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException ex, HttpServletRequest req) {
        return build(ex.getStatus(), ex.getMessage(), ex.getFieldErrors(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fields.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Please correct the highlighted fields.", fields, req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "The request body is missing or malformed.", Map.of(), req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleDenied(AccessDeniedException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "You do not have permission to access this resource.", Map.of(), req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        // Safety net for race conditions on the unique constraints (email / employee_id).
        return build(HttpStatus.CONFLICT, "An account with these details already exists.", Map.of(), req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception ex, HttpServletRequest req) {
        // Spring MVC's own errors (404 unknown route, 405 wrong method, 415, ...) keep their status.
        if (ex instanceof ErrorResponse er) {
            HttpStatus status = HttpStatus.valueOf(er.getStatusCode().value());
            return build(status, status.is4xxClientError() ? "The request could not be processed." : "Request failed.", Map.of(), req);
        }
        log.error("Unhandled error on {}", req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong on our side. Please try again.", Map.of(), req);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message,
                                           Map<String, String> fields, HttpServletRequest req) {
        ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(),
                message, fields, req.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
