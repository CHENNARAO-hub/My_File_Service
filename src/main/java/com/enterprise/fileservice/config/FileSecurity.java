package com.enterprise.fileservice.config;

import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.enterprise.fileservice.repository.FileMetaDatarepository;
import com.enterprise.fileservice.repository.UserFileRepository;

@Component("fileSecurity")
public class FileSecurity {

    private final UserFileRepository repository;

    public FileSecurity(UserFileRepository repository) {
        this.repository = repository;
    }

    public boolean isOwner(String fileId) {

        try {

            // Logged-in usersg
            Jwt jwt = (Jwt) SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getPrincipal();

            String currentUser =
                    jwt.getClaim("preferred_username");

            // Convert String -> UUID
           // UUID uuid = UUID.fromString(fileId);
            UUID uuid = parseUUID(fileId);
            // Fetch owner
            String uploadedBy =
                    repository.findOwnerUsername(uuid);

            if (uploadedBy == null) {
                 return false;
            }

            // Compare usernames
            return uploadedBy.equals(currentUser);

        } catch (Exception e) {

            e.printStackTrace();

            return false;
        }
    }
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
}