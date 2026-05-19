package com.enterprise.fileservice.service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.enterprise.fileservice.annotation.Auditable;
import com.enterprise.fileservice.entity.AuditLog;
import com.enterprise.fileservice.repository.AuditLogRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;
    @Async("auditExecutor")
    public void log(ProceedingJoinPoint joinPoint, Auditable auditable, String status,String errorMessage) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Resolve a human-friendly username. In some setups (JWT-based) auth.getName()
        // returns the subject (UUID). If the principal is a Jwt we can read
        // a preferred_username or name claim which contains the displayed username.
        String username = "anonymous";
        if (auth != null) {
            Object principal = auth.getPrincipal();
            try {
                if (principal instanceof Jwt) {
                    Jwt jwt = (Jwt) principal;
                    // Common claim names: preferred_username, name, or sub
                    String preferred = jwt.getClaimAsString("preferred_username");
                    if (preferred == null) preferred = jwt.getClaimAsString("name");
                    if (preferred == null) preferred = jwt.getSubject();
                    username = (preferred != null) ? preferred : auth.getName();
                } else {
                    // Fallback: auth.getName() (may be a UUID in some setups)
                    username = (auth.getName() != null) ? auth.getName() : "anonymous";
                }
            } catch (Exception e) {
                // Defensive: never let auditing fail because of principal parsing
                username = (auth.getName() != null) ? auth.getName() : "anonymous";
            }
        }

        String ip = "N/A";
        try {
            HttpServletRequest request =
              ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            ip = request.getRemoteAddr();
        } catch (Exception ignored) {}
try {
        AuditLog log = new AuditLog();
        log.setUsername(username);
        log.setAction(auditable.action());
        log.setMethodName(joinPoint.getSignature().getName());
        log.setStatus(status);
        log.setIpAddress(ip);
        log.setErrorMessage(errorMessage);   // 🔥 important

        log.setTimestamp(LocalDateTime.now());

        repository.save(log);
}catch (Exception ex) {

    // 🔥 fallback (VERY IMPORTANT)
    System.err.println("Audit DB failed: " + ex.getMessage());

    // Optional: save to file
    try {
        Files.writeString(
            Paths.get("audit-fallback.log"),
            "FAILED AUDIT: " + ex.getMessage() + "\n",
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        );
    } catch (Exception ignored) {
    	}
    }

    }
}