package com.enterprise.fileservice.repository;

import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.repository.JpaRepository;

import com.enterprise.fileservice.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

	// Derived query to find audit logs by username (property name is 'username')
	List<AuditLog> findByUsername(String username);

}