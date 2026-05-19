package com.enterprise.fileservice.DTO;


import lombok.*;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileUploadResponse {
    private UUID fileId;
    private String firstName;
    private String lastName;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String fileSizeReadable;   // ← NEW  e.g. "2.5 MB"
    private String uploadedAt;
    private String uploadStatus;       // ← NEW  "SUCCESS"
    private String message;
    
    
}
