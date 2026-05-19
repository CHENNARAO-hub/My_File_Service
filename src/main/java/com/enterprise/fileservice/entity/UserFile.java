package com.enterprise.fileservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "user_files",
    indexes = {
        @Index(name = "idx_user_firstname", columnList = "firstName"),
        @Index(name = "idx_user_lastname",  columnList = "lastName"),
        @Index(name = "idx_created_at",     columnList = "createdAt")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFile {

    // ─────────────────────────────────────────────
    // Primary Key
    // ─────────────────────────────────────────────
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    // ─────────────────────────────────────────────
    // User Info — WHO uploaded
    // ─────────────────────────────────────────────
    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 100)
    @Column(nullable = false, length = 100)
    private String firstName;
 // ── ADD THIS FIELD ──────────────────────────
 // Stores the exact Keycloak username from JWT
 // This is the source of truth for ownership checks
 @Column(length = 150)
 private String keycloakUsername;
 // ────────────────────────────────────────────

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 100)
    @Column(nullable = false, length = 100)
    private String lastName;

    // ─────────────────────────────────────────────
    // Soft Delete
    // ─────────────────────────────────────────────
    @Column(nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @Column
    private LocalDateTime deletedAt;

    // ─────────────────────────────────────────────
    // Audit Timestamps
    // ─────────────────────────────────────────────
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    @Column(name = "EXTRACTED_TEXT", columnDefinition = "CLOB")
    private String extractedText;

    // ─────────────────────────────────────────────
    // Optimistic Locking
    // ─────────────────────────────────────────────
    @Version
    private Long version;

    // ─────────────────────────────────────────────
    // Relationship — One UserFile has One FileMetadata
    // ─────────────────────────────────────────────
    @OneToOne(mappedBy = "userFile",
              cascade = CascadeType.ALL,
              fetch = FetchType.LAZY,
              orphanRemoval = true)
    @JsonManagedReference   // ✅ ADD THIS

    private FileMetadata fileMetadata;

    // ─────────────────────────────────────────────
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
        if (this.deleted == null) {
            this.deleted = false;
        }
    }

    // ─────────────────────────────────────────────
    public String getFullName() {
        return firstName + " " + lastName;
    }

    public void markDeleted() {
        this.deleted   = true;
        this.deletedAt = LocalDateTime.now();
    }
}