package com.example.demo.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErrorDetail> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toErrorDetail)
                .toList();
        log.info("Validation failed on {} {} with {} error(s)", request.getMethod(), request.getRequestURI(), errors.size());
        return toResponse(HttpStatus.BAD_REQUEST, "Validation failed", request, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        List<ErrorDetail> errors = ex.getConstraintViolations().stream()
                .map(this::toErrorDetail)
                .collect(Collectors.toList());
        log.info("Constraint violation on {} {} with {} error(s)", request.getMethod(), request.getRequestURI(), errors.size());
        return toResponse(HttpStatus.BAD_REQUEST, "Validation failed", request, errors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        ErrorDetail error = new ErrorDetail(ex.getName(), "Invalid value");
        log.info("Type mismatch on {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return toResponse(HttpStatus.BAD_REQUEST, "Invalid request parameter", request, List.of(error));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.info("Unreadable request body on {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return toResponse(HttpStatus.BAD_REQUEST, "Malformed JSON request", request, null);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        HttpStatus effectiveStatus = status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
        String message = Optional.ofNullable(ex.getReason()).orElseGet(effectiveStatus::getReasonPhrase);
        log.info("Handled ResponseStatusException {} {} -> {}", request.getMethod(), request.getRequestURI(), effectiveStatus);
        ErrorResponse body = toResponse(effectiveStatus, message, request, null);
        return ResponseEntity.status(effectiveStatus).body(body);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return toResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", request, null);
    }

    private ErrorDetail toErrorDetail(FieldError error) {
        return new ErrorDetail(error.getField(), Optional.ofNullable(error.getDefaultMessage()).orElse("Invalid value"));
    }

    private ErrorDetail toErrorDetail(ConstraintViolation<?> violation) {
        String field = violation.getPropertyPath() != null ? violation.getPropertyPath().toString() : null;
        return new ErrorDetail(field, violation.getMessage());
    }

    private ErrorResponse toResponse(HttpStatus status, String message, HttpServletRequest request, List<ErrorDetail> errors) {
        return new ErrorResponse(status.value(), message, request.getRequestURI(), errors);
    }

    public record ErrorResponse(int status, String message, String path, List<ErrorDetail> errors) { }

    public record ErrorDetail(String field, String message) { }
}
