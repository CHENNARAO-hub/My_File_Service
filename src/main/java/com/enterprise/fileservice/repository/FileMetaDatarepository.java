package com.enterprise.fileservice.repository;

import com.enterprise.fileservice.entity.FileMetadata;
import com.enterprise.fileservice.entity.UserFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface FileMetaDatarepository extends JpaRepository<FileMetadata, UUID> {

    // Find by linked UserFile object
    Optional<FileMetadata> findByUserFile(UserFile userFile);

    // Find by UserFile ID directly — most used
    @Query("SELECT f FROM FileMetadata f WHERE f.userFile.id = :userFileId")
    Optional<FileMetadata> findByUserFileId(@Param("userFileId") UUID userFileId);

    // ✅ Oracle-compatible EXISTS check — no LIMIT/FETCH FIRST
    @Query("SELECT COUNT(f) > 0 FROM FileMetadata f WHERE f.checksum = :checksum")
    boolean existsByChecksum(@Param("checksum") String checksum);
    
  

}