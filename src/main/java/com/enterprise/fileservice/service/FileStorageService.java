package com.enterprise.fileservice.service;

import com.enterprise.fileservice.DTO.AuditLogResponse;
import com.enterprise.fileservice.DTO.FileSearchResponse;
import com.enterprise.fileservice.DTO.FileUploadResponse;
import com.enterprise.fileservice.DTO.PagedResponse;
import com.enterprise.fileservice.annotation.Auditable;
import com.enterprise.fileservice.entity.AuditLog;
import com.enterprise.fileservice.entity.FileMetadata;
import com.enterprise.fileservice.entity.UserFile;
import com.enterprise.fileservice.exceptions.FileNotFoundException;
import com.enterprise.fileservice.repository.AuditLogRepository;
import com.enterprise.fileservice.repository.FileMetaDatarepository;
import com.enterprise.fileservice.repository.UserFileRepository;
import com.enterprise.fileservice.utils.JwtUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final UserFileRepository     userFileRepository;
    private final AuditLogRepository     auditLogRepository;
    private final FileMetaDatarepository fileMetadataRepository;
    private final JwtUtils jwtUtils;   // ← inject JwtUtils
    private final PdfService pdfService;
    // ─────────────────────────────────────────────
    // UPLOAD
    // ─────────────────────────────────────────────

    @Transactional
    @Caching(evict = {
    	    @CacheEvict(value = "fileByName", allEntries = true)
    	})

    @Auditable(action = "UPLOAD_FILE")
    public FileUploadResponse uploadFile(String firstName,
                                         String lastName,
                                         MultipartFile file) throws IOException {

        // 1. Validate file
        validateFile(file);
        // Extract Keycloak username automatically from JWT
        // No need to ask user to type it — it comes from the token
        String keycloakUsername = jwtUtils.getCurrentUsername();

        // 2. Read bytes once — reuse for checksum + disk write
        byte[] fileBytes = file.getBytes();

        // 3. Compute SHA-256 checksum
        String checksum = computeChecksum(fileBytes);
        log.info("Computed checksum: {}", checksum);

        // 4. Duplicate detection
        if (fileMetadataRepository.existsByChecksum(checksum)) {
            log.warn("Duplicate upload by {} {} — checksum: {}", firstName, lastName, checksum);
            throw new IllegalArgumentException(
                    "Duplicate file — this file already exists in the system.");
        }

        // 5. Ensure upload directory exists
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        // 6. Generate UUID stored filename
        String originalFileName = file.getOriginalFilename();
        String extension        = getFileExtension(originalFileName);
        String storedFileName   = UUID.randomUUID() + "." + extension;
        Path   targetPath       = uploadPath.resolve(storedFileName);

     // 7. Write file to disk
        Files.write(targetPath, fileBytes, StandardOpenOption.CREATE_NEW);
        log.info("File saved to disk: {}", targetPath);

        // 8. Resolve correct MIME type
        String resolvedType = resolveContentType(
                file.getContentType(),
                originalFileName);

        log.info("Resolved content type: {}", resolvedType);

        // 9. Extract document text for AI
        String extractedText = "";

        if ("application/pdf".equalsIgnoreCase(resolvedType)) {

            extractedText = pdfService.extractText(file);

            log.info("PDF text extracted successfully");
        }

        UserFile userFile = UserFile.builder()
                .firstName(firstName)
                .lastName(lastName)
                .keycloakUsername(keycloakUsername)
                .extractedText(extractedText)
                .deleted(false)
                .build();

        UserFile savedUser = userFileRepository.save(userFile);
        log.info("UserFile saved — id: {}", savedUser.getId());

        // 10. Save FileMetadata — WHAT was uploaded
        FileMetadata fileMetadata = FileMetadata.builder()
                .userFile(savedUser)
                .originalFileName(originalFileName)
                .storedFileName(storedFileName)
                .filePath(targetPath.toString())
                .fileType(resolvedType)             // ✅ use resolved type
                .fileSize(file.getSize())
                .checksum(checksum)               

                .uploadStatus("SUCCESS")
                .downloadCount(0L)
                .build();

        FileMetadata savedMeta = fileMetadataRepository.save(fileMetadata);
        log.info("FileMetadata saved — id: {}", savedMeta.getId());

        // 11. Return response
        return FileUploadResponse.builder()
                .fileId(savedUser.getId())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .fileName(savedMeta.getOriginalFileName())
                .fileType(savedMeta.getFileType())
                .fileSize(savedMeta.getFileSize())
                .fileSizeReadable(savedMeta.getFileSizeReadable())
                .uploadedAt(savedMeta.getUploadedAt() != null
                        ? savedMeta.getUploadedAt().toString()
                        : LocalDateTime.now().toString())
                .uploadStatus(savedMeta.getUploadStatus() != null
                        ? savedMeta.getUploadStatus()
                        : "SUCCESS")
                .message("File uploaded successfully ✅")
                .build();
    }

    // ─────────────────────────────────────────────
    // DOWNLOAD
    // ─────────────────────────────────────────────

    @Transactional
    @Auditable(action = "DOWNLOAD_FILE")

    public Resource downloadFile(String userFileId) throws MalformedURLException {
        UUID id = parseUUID(userFileId);

        UserFile userFile = userFileRepository.findById(id)
                .orElseThrow(() -> new FileNotFoundException(
                        "File not found with id: " + userFileId));

        if (Boolean.TRUE.equals(userFile.getDeleted())) {
            log.warn("Download attempt on deleted file: {}", userFileId);
            throw new FileNotFoundException(
                    "File has been deleted: " + userFileId);
        }

        FileMetadata metadata = fileMetadataRepository
                .findByUserFileId(id)
                .orElseThrow(() -> new FileNotFoundException(
                        "File metadata not found for id: " + userFileId));

        Path filePath = Paths.get(metadata.getFilePath()).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            log.error("File missing from disk — id: {} | path: {}", userFileId, filePath);
            throw new FileNotFoundException(
                    "File not found on disk. Contact support. ID: " + userFileId);
        }

        metadata.recordDownload();
        fileMetadataRepository.save(metadata);

        log.info("File downloaded — id: {} | total: {}", userFileId, metadata.getDownloadCount());

        return resource;
    }

    // ─────────────────────────────────────────────
    // GET METADATA
    // ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    @Cacheable(value = "fileMetadata", key = "#userFileId")
    public UserFile getFileMetadata(String userFileId) {
        UUID id = parseUUID(userFileId);

        UserFile userFile = userFileRepository.findByIdWithMetadata(id)
                .orElseThrow(() -> new FileNotFoundException(
                        "File not found: " + userFileId));

        if (Boolean.TRUE.equals(userFile.getDeleted())) {
            throw new FileNotFoundException("File has been deleted: " + userFileId);
        }

        return userFile;
    }

    // ─────────────────────────────────────────────
    // GET FILE METADATA ONLY
    // ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    @Cacheable(value = "fileMetadataOnly", key = "#userFileId")
    public FileMetadata getFileMetadataOnly(String userFileId) {
        UUID id = parseUUID(userFileId);

        return fileMetadataRepository
                .findByUserFileId(id)
                .orElseThrow(() -> new FileNotFoundException(
                        "Metadata not found for id: " + userFileId));
    }

    // ─────────────────────────────────────────────
    // GET FILES BY NAME
    // ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    @Cacheable(value = "fileByName", key = "#firstName + '_' + #lastName")
    public List<UserFile> getFilesByName(String firstName, String lastName) {
        List<UserFile> files = userFileRepository
                .findByFirstNameAndLastNameAndDeletedFalse(firstName, lastName);
        log.info("Found {} file(s) for: {} {}", files.size(), firstName, lastName);
        return files;
    }

    // ─────────────────────────────────────────────
    // SOFT DELETE
    // ─────────────────────────────────────────────

    @Transactional
    @Caching(evict = {
    	    @CacheEvict(value = "fileMetadata", key = "#userFileId"),
    	    @CacheEvict(value = "fileMetadataOnly", key = "#userFileId"),
    	    @CacheEvict(value = "fileByName", allEntries = true)
    	})

    @Auditable(action = "DELETE_FILE")
    
    public void deleteFile(String userFileId) {
        UUID id = parseUUID(userFileId);

        UserFile userFile = userFileRepository.findById(id)
                .orElseThrow(() -> new FileNotFoundException(
                        "File not found: " + userFileId));

        if (Boolean.TRUE.equals(userFile.getDeleted())) {
            throw new IllegalStateException("File is already deleted: " + userFileId);
        }

        userFile.markDeleted();
        userFileRepository.save(userFile);
        log.info("File soft-deleted — id: {}", userFileId);
    }

    // ─────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────

    private void validateFile(MultipartFile file) {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload an empty file.");
        }

        long maxSizeBytes = 10L * 1024 * 1024;
        if (file.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException("File size exceeds 10MB limit.");
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new IllegalArgumentException("File name is missing or blank.");
        }

        // Path traversal attack prevention
        if (originalFileName.contains("..") ||
            originalFileName.contains("/")  ||
            originalFileName.contains("\\")) {
            throw new IllegalArgumentException(
                    "Invalid file name — path traversal detected.");
        }

        // ✅ Resolve type first — then validate
        String resolvedType = resolveContentType(file.getContentType(), originalFileName);
        if (!isAllowedType(resolvedType)) {
            throw new IllegalArgumentException(
                    "File type not allowed. Allowed: PDF, JPEG, PNG, DOC, DOCX");
        }
    }

    // ✅ Resolves MIME type from extension when Postman/browser sends wrong type
    private String resolveContentType(String contentType, String fileName) {
        if (contentType == null
                || contentType.isBlank()
                || contentType.equalsIgnoreCase("application/octet-stream")
                || contentType.equalsIgnoreCase("File")) {

            String ext = getFileExtension(fileName).toLowerCase();
            log.info("Resolving content type from extension: .{}", ext);

            return switch (ext) {
                case "pdf"          -> "application/pdf";
                case "jpg", "jpeg"  -> "image/jpeg";
                case "png"          -> "image/png";
                case "doc"          -> "application/msword";
                case "docx"         -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                default             -> contentType != null ? contentType : "unknown";
            };
        }
        return contentType;
    }

    private boolean isAllowedType(String contentType) {
        return switch (contentType) {
            case "application/pdf",
                 "image/jpeg",
                 "image/png",
                 "application/msword",
                 "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    -> true;
            default -> false;
        };
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "bin";
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    private String computeChecksum(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 not found — critical", e);
            throw new RuntimeException("Checksum computation failed", e);
        }
    }
 // ─────────────────────────────────────────────
 // UUID HELPER — Convert Oracle RAW to proper UUID
 // ─────────────────────────────────────────────
 private UUID parseUUID(String id) {
     if (id == null || id.isBlank()) {
         throw new IllegalArgumentException("ID cannot be blank");
     }
     // Already has dashes — use directly
     if (id.contains("-")) {
         return UUID.fromString(id);
     }
     // Oracle RAW format — add dashes
     // FD879250E73743E6BFF06EB607E68B83
     // → FD879250-E737-43E6-BFF0-6EB607E68B83
     if (id.length() == 32) {
         String withDashes = id.substring(0, 8)  + "-" +
                             id.substring(8, 12)  + "-" +
                             id.substring(12, 16) + "-" +
                             id.substring(16, 20) + "-" +
                             id.substring(20);
         return UUID.fromString(withDashes);
     }
     throw new IllegalArgumentException("Invalid ID format: " + id);
 }
//─────────────────────────────────────────────
//GET FILES BY NAME — Paginated + Sorted
//─────────────────────────────────────────────

 @Transactional(readOnly = true)
 public PagedResponse<FileSearchResponse> getFilesByNamePaged(
         String firstName,
         String lastName,
         int page,
         int size,
         String sortBy,
         String sortDir) {

     // 1. Validate params
     size = Math.min(Math.max(size, 1), 50);
     page = Math.max(page, 0);
     String safeSortBy = validateSortField(sortBy);

     // 2. Sort direction
     Sort.Direction direction = sortDir != null &&
             sortDir.equalsIgnoreCase("asc")
             ? Sort.Direction.ASC
             : Sort.Direction.DESC;

     // 3. ✅ Oracle 11g fix — fetch ALL matching records
     //    then paginate manually in Java
     List<UserFile> allFiles = userFileRepository
             .findByFirstNameAndLastNameAndDeletedFalse(firstName, lastName);

     // 4. Sort manually
     allFiles.sort((a, b) -> {
         int result = switch (safeSortBy) {
             case "firstName" -> a.getFirstName()
                     .compareToIgnoreCase(b.getFirstName());
             case "lastName"  -> a.getLastName()
                     .compareToIgnoreCase(b.getLastName());
             case "updatedAt" -> {
                 if (a.getUpdatedAt() == null) yield 1;
                 if (b.getUpdatedAt() == null) yield -1;
                 yield a.getUpdatedAt().compareTo(b.getUpdatedAt());
             }
             default -> {  // createdAt
                 if (a.getCreatedAt() == null) yield 1;
                 if (b.getCreatedAt() == null) yield -1;
                 yield a.getCreatedAt().compareTo(b.getCreatedAt());
             }
         };
         // Apply direction
         return direction == Sort.Direction.DESC ? -result : result;
     });

     // 5. Calculate pagination
     long totalElements = allFiles.size();
     int  totalPages    = (int) Math.ceil((double) totalElements / size);
     int  fromIndex     = page * size;
     int  toIndex       = Math.min(fromIndex + size, (int) totalElements);

     // 6. Handle out of range page
     if (fromIndex >= totalElements) {
         return PagedResponse.<FileSearchResponse>builder()
                 .content(List.of())
                 .currentPage(page)
                 .totalPages(totalPages)
                 .totalElements(totalElements)
                 .pageSize(size)
                 .isFirst(page == 0)
                 .isLast(true)
                 .hasNext(false)
                 .hasPrevious(page > 0)
                 .sortBy(safeSortBy)
                 .sortDir(direction.name().toLowerCase())
                 .build();
     }

     // 7. Slice the list for current page
     List<UserFile> pageContent = allFiles.subList(fromIndex, toIndex);

     log.info("Search — name: {} {} | page: {}/{} | size: {} | " +
              "total: {} | sort: {} {}",
             firstName, lastName,
             page + 1, totalPages,
             size, totalElements,
             safeSortBy, direction);

     // 8. Map to DTO
     List<FileSearchResponse> content = pageContent
             .stream()
             .map(this::mapToSearchResponse)
             .toList();

     // 9. Return paged response
     return PagedResponse.<FileSearchResponse>builder()
             .content(content)
             .currentPage(page)
             .totalPages(totalPages)
             .totalElements(totalElements)
             .pageSize(size)
             .isFirst(page == 0)
             .isLast(page >= totalPages - 1)
             .hasNext(page < totalPages - 1)
             .hasPrevious(page > 0)
             .sortBy(safeSortBy)
             .sortDir(direction.name().toLowerCase())
             .build();
 }

//─────────────────────────────────────────────
//HELPER — Map UserFile to FileSearchResponse
//─────────────────────────────────────────────
private FileSearchResponse mapToSearchResponse(UserFile u) {
  FileSearchResponse response = FileSearchResponse.builder()
          .fileId(u.getId())
          .firstName(u.getFirstName())
          .lastName(u.getLastName())
          .fullName(u.getFullName())
          .createdAt(u.getCreatedAt() != null
                  ? u.getCreatedAt().toString() : null)
          .deleted(u.getDeleted())
          .build();

  // Safely get FileMetadata if present
  if (u.getFileMetadata() != null) {
      FileMetadata m = u.getFileMetadata();
      response.setFileName(m.getOriginalFileName());
      response.setFileType(m.getFileType());
      response.setFileSize(m.getFileSize());
      response.setFileSizeReadable(m.getFileSizeReadable());
      response.setUploadStatus(m.getUploadStatus());
      response.setUploadedAt(m.getUploadedAt() != null
              ? m.getUploadedAt().toString() : null);
      response.setDownloadCount(m.getDownloadCount());
      response.setLastAccessedAt(m.getLastAccessedAt() != null
              ? m.getLastAccessedAt().toString() : "Never");
  }

  return response;
}

//─────────────────────────────────────────────
//HELPER — Validate sort field (prevent injection)
//─────────────────────────────────────────────
private String validateSortField(String sortBy) {
  // Only allow these fields for sorting
  List<String> allowedFields = List.of(
          "createdAt",    // default
          "firstName",
          "lastName",
          "updatedAt",
          "deletedAt"
  );
  if (sortBy == null || !allowedFields.contains(sortBy)) {
      return "createdAt"; // default sort
  }
  return sortBy;
}

 public List<AuditLogResponse> getAuditLogs(String username) {
    // If username is blank or not provided, return an empty list (do NOT return all logs)
    if (username == null || username.isBlank()) {
        return List.of();
    }

    List<AuditLog> logs = auditLogRepository.findByUsername(username);
    return logs.stream()
            .map(l -> AuditLogResponse.builder()
                    .username(l.getUsername())
                    .action(l.getAction())
                    .errorMessage(l.getErrorMessage())
                    .status(l.getStatus())
                    .timestamp(l.getTimestamp() != null ? l.getTimestamp().toString() : null)
                    .build())
            .collect(Collectors.toList());
 }
//─────────────────────────────────────────────
//GET DOCUMENT TEXT FOR AI
//─────────────────────────────────────────────

@Transactional(readOnly = true)
public String getDocumentText(String userFileId) {

  UUID id = parseUUID(userFileId);

  UserFile userFile = userFileRepository.findById(id)
          .orElseThrow(() ->
                  new FileNotFoundException(
                          "File not found: " + userFileId));

  if (Boolean.TRUE.equals(userFile.getDeleted())) {
      throw new FileNotFoundException(
              "File has been deleted: " + userFileId);
  }

  return userFile.getExtractedText();
}

}