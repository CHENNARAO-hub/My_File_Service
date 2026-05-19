package com.enterprise.fileservice.exceptions;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.AccessDeniedException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptions {

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<?> handleFileNotFound(FileNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }
    // ─── Ownership violation ───────────────────────────────────────────
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "status", 403,
                        "errorCode", "ACCESS_DENIED",
                        "message", "You are not allowed to access this file"
                ));
    }

}