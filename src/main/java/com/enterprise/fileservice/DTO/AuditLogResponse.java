package com.enterprise.fileservice.DTO;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
	
		private String username;
	private String action;
	private String errorMessage;
	private String status;
	private String timestamp;

	
}
