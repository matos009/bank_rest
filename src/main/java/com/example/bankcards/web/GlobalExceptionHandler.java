package com.example.bankcards.web;

import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.exception.ConflictException;
import com.example.bankcards.exception.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private Map<String, Object> body(String code, String message, HttpStatus status) {
        return Map.of(
                "timestamp", OffsetDateTime.now().toString(),
                "status", status.value(),
                "error", code,
                "message", message
        );
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleNotFound(NotFoundException ex) {
        return body("NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleConflict(ConflictException ex) {
        return body("CONFLICT", ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleBusiness(BusinessException ex) {
        return body("BAD_REQUEST", ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    // на всё прочее — 500
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleOther(Exception ex) {
        return body("INTERNAL_ERROR", "Unexpected error", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}