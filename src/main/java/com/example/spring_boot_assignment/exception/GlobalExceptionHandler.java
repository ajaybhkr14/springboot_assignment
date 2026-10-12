package com.example.spring_boot_assignment.exception;

import com.example.spring_boot_assignment.model.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NodeNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NodeNotFoundException e, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "NODE_NOT_FOUND", e.getMessage(), request);
    }

    @ExceptionHandler(CycleDetectedException.class)
    public ResponseEntity<ApiErrorResponse> handleCycle(CycleDetectedException e, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "CYCLE_DETECTED", e.getMessage(), request);
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalid(InvalidRequestException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", e.getMessage(), request);
    }


    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String error, String message,
                                                   HttpServletRequest request) {
        return ResponseEntity.status(status).body(body(status, error, message, request));
    }

    private ApiErrorResponse body(HttpStatus status, String error, String message, HttpServletRequest request) {
        return new ApiErrorResponse(error, message, status.value(), request.getRequestURI(),
            Instant.now().truncatedTo(ChronoUnit.MILLIS).toString());
    }
}
