package com.studysyncer.backend.web.api;

import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice(annotations = RestController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RestExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fieldErrors.put(fe.getField(), fe.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of(
                "ok", false,
                "fieldErrors", fieldErrors,
                "message", "Please fix the highlighted fields."));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, Object>> binding(BindException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fieldErrors.put(fe.getField(), fe.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of(
                "ok", false,
                "fieldErrors", fieldErrors,
                "message", "Please fix the highlighted fields."));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> notFound(NoSuchElementException ex) {
        return ResponseEntity.status(404).body(Map.of("ok", false, "message", "Not found."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> forbidden(AccessDeniedException ex) {
        return ResponseEntity.status(403).body(Map.of("ok", false, "message", "Forbidden."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badInput(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "ok", false,
                "message", ex.getMessage() != null ? ex.getMessage() : "Bad request."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> generic(Exception ex) {
        LoggerFactory.getLogger(RestExceptionHandler.class).error("API error", ex);
        return ResponseEntity.status(500).body(Map.of(
                "ok", false,
                "message", "Something went wrong. Please try again."));
    }
}
