package com.enterprise.fileservice.controller;

import com.enterprise.fileservice.DTO.FileSearchResponse;
import com.enterprise.fileservice.DTO.PagedResponse;
import com.enterprise.fileservice.entity.FileMetadata;
import com.enterprise.fileservice.entity.UserFile;
import com.enterprise.fileservice.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final FileStorageService fileStorageService;

    // ─────────────────────────────────────────────
    // GET FILES — Paginated + Sorted
    // GET /api/v1/users/files?firstName=John&lastName=Doe
    //   &page=0&size=10&sortBy=createdAt&sortDir=desc
    // ─────────────────────────────────────────────
    @GetMapping("/files")
    public ResponseEntity<PagedResponse<FileSearchResponse>> getFilesByName(

            @RequestParam String firstName,
            @RequestParam String lastName,

            // Pagination params — all have defaults
            @RequestParam(defaultValue = "0")          int    page,
            @RequestParam(defaultValue = "5")         int    size,
            @RequestParam(defaultValue = "createdAt")  String sortBy,
            @RequestParam(defaultValue = "desc")       String sortDir) {

        log.info("Search — name: {} {} | page: {} | size: {} | " +
                 "sort: {} {}", firstName, lastName, page, size, sortBy, sortDir);

        PagedResponse<FileSearchResponse> response =
                fileStorageService.getFilesByNamePaged(
                        firstName, lastName,
                        page, size, sortBy, sortDir);

        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────
    // GET USER + FILE BY ID
    // ─────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<UserWithFileResponse> getUserById(
            @PathVariable String id) {

        log.info("Fetching user+file by id: {}", id);

        UserFile     userFile = fileStorageService.getFileMetadata(id);
        FileMetadata fileMeta = fileStorageService.getFileMetadataOnly(id);

        UserWithFileResponse response = UserWithFileResponse.builder()
                .fileId(userFile.getId())
                .firstName(userFile.getFirstName())
                .lastName(userFile.getLastName())
                .fullName(userFile.getFullName())
                .createdAt(userFile.getCreatedAt().toString())
                .fileName(fileMeta.getOriginalFileName())
                .fileType(fileMeta.getFileType())
                .fileSize(fileMeta.getFileSize())
                .fileSizeReadable(fileMeta.getFileSizeReadable())
                .uploadStatus(fileMeta.getUploadStatus())
                .uploadedAt(fileMeta.getUploadedAt().toString())
                .downloadCount(fileMeta.getDownloadCount())
                .lastAccessedAt(fileMeta.getLastAccessedAt() != null
                        ? fileMeta.getLastAccessedAt().toString() : "Never")
                .build();

        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────
    // INNER DTO
    // ─────────────────────────────────────────────
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UserWithFileResponse {
        private UUID   fileId;
        private String firstName;
        private String lastName;
        private String fullName;
        private String createdAt;
        private String fileName;
        private String fileType;
        private Long   fileSize;
        private String fileSizeReadable;
        private String uploadStatus;
        private String uploadedAt;
        private Long   downloadCount;
        private String lastAccessedAt;
    }
}