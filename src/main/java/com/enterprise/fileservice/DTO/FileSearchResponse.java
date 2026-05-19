package com.enterprise.fileservice.DTO;


import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileSearchResponse {

    private UUID   fileId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String createdAt;
    private String deletedAt;
    private Boolean deleted;

    // File info from FileMetadata
    private String fileName;
    private String fileType;
    private Long   fileSize;
    private String fileSizeReadable;
    private String uploadStatus;
    private String uploadedAt;
    private Long   downloadCount;
    private String lastAccessedAt;
}
