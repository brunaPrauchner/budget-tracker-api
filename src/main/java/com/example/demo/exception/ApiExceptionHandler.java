package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorDetail> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toErrorDetail)
                .toList();
        return new ErrorResponse("Validation failed", errors);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        ErrorResponse body = new ErrorResponse(ex.getReason(), null);
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneric(Exception ex) {
        return new ErrorResponse("Unexpected error", null);
    }

    private ErrorDetail toErrorDetail(FieldError error) {
        return new ErrorDetail(error.getField(), error.getDefaultMessage());
    }

    public record ErrorResponse(String message, List<ErrorDetail> errors) { }

    public record ErrorDetail(String field, String message) { }
}
