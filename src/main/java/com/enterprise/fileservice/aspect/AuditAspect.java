package com.enterprise.fileservice.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import com.enterprise.fileservice.annotation.Auditable;
import com.enterprise.fileservice.service.AuditService;

import lombok.RequiredArgsConstructor;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {

        String status = "SUCCESS";
        Object result;
        String errorMessage = null;


        try {
            result = joinPoint.proceed();
        } catch (Exception ex) {
            status = "FAILURE";
            errorMessage = ex.getMessage();   // 🔥 capture error

            throw ex;
        } finally {
            auditService.log(joinPoint, auditable, status,errorMessage);
        }

        return result;
    }
}