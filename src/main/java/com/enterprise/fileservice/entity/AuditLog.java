package com.enterprise.fileservice.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_seq")
    @SequenceGenerator(name = "audit_seq", sequenceName = "audit_logs_seq", allocationSize = 1)
    private Long id;

    private String username;
    private String action;
    @Column(name = "METHOD_NAME")
    private String methodName;
    @Column(name = "ERROR_MESSAGE")
    private String errorMessage;
    private String status;
    
    @Column(name = "IP_ADDRESS")
    private String ipAddress;

    private LocalDateTime timestamp;

    // getters & setters
}