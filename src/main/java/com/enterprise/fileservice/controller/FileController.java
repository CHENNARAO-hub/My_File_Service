package com.enterprise.fileservice.controller;

import com.enterprise.fileservice.DTO.FileUploadResponse;
import com.enterprise.fileservice.DTO.AuditLogResponse;
import com.enterprise.fileservice.entity.FileMetadata;
import com.enterprise.fileservice.entity.UserFile;
import com.enterprise.fileservice.service.FileStorageService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5178")
public class FileController {
    private final FileStorageService fileStorageService;

    // ─────────────────────────────────────────────
    // UPLOAD
    // POST /api/v1/files/upload
    // ─────────────────────────────────────────────
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")

    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("firstName")
            @NotBlank(message = "First name must not be blank") String firstName,

            @RequestParam("lastName")
            @NotBlank(message = "Last name must not be blank") String lastName,

            @RequestParam("file") MultipartFile file) throws IOException {

        log.info("Upload request — user: {} {}, file: {}, size: {} bytes",
                firstName, lastName,
                file.getOriginalFilename(),
                file.getSize());

        FileUploadResponse response = fileStorageService.uploadFile(firstName, lastName, file);

        log.info("Upload successful — fileId: {}", response.getFileId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // ─────────────────────────────────────────────
    // DOWNLOAD
    // GET /api/v1/files/download/{fileId}
    // ─────────────────────────────────────────────
    @GetMapping("/download/{fileId}")
    @PreAuthorize("@fileSecurity.isOwner(#fileId)")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String fileId) throws MalformedURLException {

        log.info("Download request — fileId: {}", fileId);

        // Fetch metadata
        FileMetadata metadata = fileStorageService.getFileMetadataOnly(fileId);

        // Fetch file resource
        Resource resource = fileStorageService.downloadFile(fileId);

        // Validate resource
        if (resource == null || !resource.exists() || !resource.isReadable()) {

            log.error("File not found or unreadable — fileId: {}", fileId);

            throw new RuntimeException("File not found");
        }

        // Safe filename
        String safeFileName =
                sanitizeFileName(metadata.getOriginalFileName());

        // Safe media type
        MediaType mediaType;

        try {

            mediaType = MediaType.parseMediaType(
                    metadata.getFileType()
            );

        } catch (Exception e) {

            log.warn("Invalid content type for fileId: {} , using default",
                    fileId);

            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        log.info("Serving file — fileId: {} | name: {} | downloads: {}",
                fileId,
                safeFileName,
                metadata.getDownloadCount());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + safeFileName + "\"")
                .header(HttpHeaders.CONTENT_LENGTH,
                        String.valueOf(
                                metadata.getFileSize() != null
                                        ? metadata.getFileSize()
                                        : 0
                        ))
                .header("X-File-Id", fileId)
                .header("X-File-Name", safeFileName)
                .body(resource);
    }

    // ─────────────────────────────────────────────
    // METADATA
    // GET /api/v1/files/metadata/{fileId}
    // ─────────────────────────────────────────────
    @GetMapping("/metadata/{fileId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")

    public ResponseEntity<FileMetadataResponse> getMetadata(
            @PathVariable String fileId) {

        log.info("Metadata request — fileId: {}", fileId);

        // ✅ UserFile has person info (firstName, lastName)
        UserFile userFile = fileStorageService.getFileMetadata(fileId);

        // ✅ FileMetadata has file technical info
        FileMetadata fileMeta = fileStorageService.getFileMetadataOnly(fileId);

        // ✅ Map to safe DTO — never expose filePath, storedFileName, checksum
        FileMetadataResponse response = FileMetadataResponse.builder()
                .fileId(userFile.getId())
                .firstName(userFile.getFirstName())
                .lastName(userFile.getLastName())
                .fullName(userFile.getFullName())
                .fileName(fileMeta.getOriginalFileName())
                .fileType(fileMeta.getFileType())
                .fileSize(fileMeta.getFileSize())
                .fileSizeReadable(fileMeta.getFileSizeReadable())
                .uploadedAt(fileMeta.getUploadedAt().toString())
                .lastAccessedAt(fileMeta.getLastAccessedAt() != null
                        ? fileMeta.getLastAccessedAt().toString() : "Never")
                .downloadCount(fileMeta.getDownloadCount())
                .uploadStatus(fileMeta.getUploadStatus())
                .build();

        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────
    // SEARCH BY NAME
    // GET /api/v1/files/search?firstName=John&lastName=Doe
    // ─────────────────────────────────────────────
    @GetMapping("/search")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")

    public ResponseEntity<?> getFilesByName(
            @RequestParam @NotBlank String firstName,
            @RequestParam @NotBlank String lastName) {

        log.info("Search request — name: {} {}", firstName, lastName);

        List<UserFile> files = fileStorageService.getFilesByName(firstName, lastName);

        if (files.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "message", "No files found for: " + firstName + " " + lastName
            ));
        }

        return ResponseEntity.ok(files);
    }

    // ─────────────────────────────────────────────
    // SOFT DELETE
    // DELETE /api/v1/files/{fileId}
    // ─────────────────────────────────────────────
    @DeleteMapping("/{fileId}")
    @PreAuthorize("@fileSecurity.isOwner(#fileId)")

    public ResponseEntity<Map<String, String>> deleteFile(
            @PathVariable String fileId) {

        log.info("Delete request — fileId: {}", fileId);

        fileStorageService.deleteFile(fileId);

        return ResponseEntity.ok(Map.of(
                "message", "File deleted successfully",
                "fileId",  fileId.toString()
        ));
    }

    // ─────────────────────────────────────────────
    // HEALTH CHECK
    // GET /api/v1/files/health
    // ─────────────────────────────────────────────
    @GetMapping("/audit")
    public ResponseEntity<List<AuditLogResponse>> getAuditLogs(@RequestParam(required = false) String username) {
        // Accept optional username. Service returns empty list when username is null/blank.
        List<AuditLogResponse> logs = fileStorageService.getAuditLogs(username);
        System.out.println("Audit logs retrieved: " + logs.size() + " entries for username: " + username);
       log.info("Audit logs retrieved: {} entries for username: {}", logs);
        return ResponseEntity.ok(logs);
    }
    @GetMapping("/health")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")

    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status",  "UP",
                "service", "FileStorageService"
        ));
    }
    @GetMapping("/debug")
    public String debug(@AuthenticationPrincipal Jwt jwt) {
        return jwt.getIssuer().toString();
    }
    // ─────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────
    private String sanitizeFileName(String fileName) {
        if (fileName == null) return "download";
        return fileName.replaceAll("[\\r\\n\"\\\\]", "_");
    }

    // ─────────────────────────────────────────────
    // INNER DTO — Safe response (hides internal paths)
    // ─────────────────────────────────────────────
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileMetadataResponse {
        private UUID   fileId;
        private String firstName;
        private String lastName;
        private String fullName;          // ← from UserFile.getFullName()
        private String fileName;          // ← from FileMetadata
        private String fileType;          // ← from FileMetadata
        private Long   fileSize;          // ← from FileMetadata
        private String fileSizeReadable;  // ← from FileMetadata
        private String uploadedAt;        // ← from FileMetadata
        private String lastAccessedAt;    // ← from FileMetadata
        private Long   downloadCount;     // ← from FileMetadata
        private String uploadStatus;      // ← from FileMetadata
        // ✅ filePath, storedFileName, checksum intentionally hidden
    }
}