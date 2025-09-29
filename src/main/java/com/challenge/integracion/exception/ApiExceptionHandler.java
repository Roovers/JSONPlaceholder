package com.challenge.integracion.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;

import java.time.Instant;

@ControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(HttpStatusCodeException.class)
    public ResponseEntity<ApiError> handleHttpStatus(HttpStatusCodeException ex, HttpServletRequest req) {
        log.warn("Error HTTP externo: {} - {}", ex.getStatusCode(), ex.getMessage());
        ApiError body = new ApiError(Instant.now(), ex.getStatusCode().value(),
                ex.getStatusText(), ex.getResponseBodyAsString(), req.getRequestURI());
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ApiError> handleTimeout(ResourceAccessException ex, HttpServletRequest req) {
        log.error("Timeout/IO al llamar API externa", ex);
        ApiError body = new ApiError(Instant.now(), HttpStatus.GATEWAY_TIMEOUT.value(),
                "Gateway Timeout", ex.getMessage(), req.getRequestURI());
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String msg = ex.getBindingResult().getAllErrors().stream()
                .findFirst().map(e -> e.getDefaultMessage()).orElse("Validacion invalida");
        ApiError body = new ApiError(Instant.now(), HttpStatus.BAD_REQUEST.value(),
                "Bad Request", msg, req.getRequestURI());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ApiError> handleRestClient(RestClientException ex, HttpServletRequest req) {
        log.error("Error de cliente REST", ex);
        ApiError body = new ApiError(Instant.now(), HttpStatus.BAD_GATEWAY.value(),
                "Bad Gateway", ex.getMessage(), req.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Error no controlado", ex);
        ApiError body = new ApiError(Instant.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error", ex.getMessage(), req.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));

        ApiError body = new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                message,
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
