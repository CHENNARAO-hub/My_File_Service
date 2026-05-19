package com.enterprise.fileservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonBackReference;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "file_metadata",
    indexes = {
        @Index(name = "idx_checksum",       columnList = "checksum"),
        @Index(name = "idx_upload_status",  columnList = "uploadStatus"),
        @Index(name = "idx_file_type",      columnList = "fileType"),
        @Index(name = "idx_uploaded_at",    columnList = "uploadedAt")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileMetadata {

    // ─────────────────────────────────────────────
    // Primary Key
    // ─────────────────────────────────────────────
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    // ─────────────────────────────────────────────
    // Relationship — belongs to UserFile
    // ─────────────────────────────────────────────
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_file_id", nullable = false, unique = true)
    @JsonBackReference      // ✅ ADD THIS

    private UserFile userFile;

    // ─────────────────────────────────────────────
    // File Info — WHAT was uploaded
    // ─────────────────────────────────────────────
    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String originalFileName;      // Name as user uploaded e.g. "report.pdf"

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String storedFileName;        // UUID renamed e.g. "a1b2c3.pdf"

    @NotBlank
    @Size(max = 500)
    @Column(nullable = false, length = 500)
    private String filePath;              // Full disk path

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String fileType;              // MIME type e.g. application/pdf

    @Positive
    @Column(nullable = false)
    private Long fileSize;                // Size in bytes

    // ─────────────────────────────────────────────
    // Security & Integrity
    // ─────────────────────────────────────────────
    @Size(max = 64)
    @Column(length = 64, unique = true)
    private String checksum;              // SHA-256 — duplicate detection

    // ─────────────────────────────────────────────
    // Status Tracking
    // ─────────────────────────────────────────────
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String uploadStatus = "SUCCESS"; // PENDING | SUCCESS | FAILED

    @Column(length = 500)
    private String uploadFailureReason;   // Why it failed

    // ─────────────────────────────────────────────
    // Access Tracking
    // ─────────────────────────────────────────────
    @Column
    private LocalDateTime lastAccessedAt; // Last download time

    @Column(nullable = false)
    @Builder.Default
    private Long downloadCount = 0L;      // Total downloads

    // ─────────────────────────────────────────────
    // Audit Timestamps
    // ─────────────────────────────────────────────
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;
    

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // ─────────────────────────────────────────────
    // Optimistic Locking
    // ─────────────────────────────────────────────
    @Version
    private Long version;
    // ─────────────────────────────────────────────
    // Lifecycle — runs before every DB insert
    // ─────────────────────────────────────────────
    @PrePersist
    public void prePersist() {
        if (this.uploadedAt == null) {
            this.uploadedAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
        if (this.uploadStatus == null) {
            this.uploadStatus = "SUCCESS";
        }
        if (this.downloadCount == null) {
            this.downloadCount = 0L;
        }
    }                                          // ✅ closing brace for prePersist()



    // ─────────────────────────────────────────────
    // Helper Methods
    // ─────────────────────────────────────────────
    public String getFileSizeReadable() {
        if (fileSize == null) return "Unknown";
        if (fileSize < 1024)        return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.2f KB", fileSize / 1024.0);
        return String.format("%.2f MB", fileSize / (1024.0 * 1024));
    }

    public void recordDownload() {
        this.downloadCount++;
        this.lastAccessedAt = LocalDateTime.now();
    }
    
    
}