package com.enterprise.fileservice.repository;

import com.enterprise.fileservice.entity.UserFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserFileRepository extends JpaRepository<UserFile, UUID> {

    // ✅ Non-paginated — kept for other uses
    List<UserFile> findByFirstNameAndLastNameAndDeletedFalse(
            String firstName, String lastName);

    // ✅ Oracle 11g compatible — JPQL with Pageable
    // Spring Data handles ROWNUM internally when dialect is auto-detected
    @Query(value = "SELECT u FROM UserFile u " +
                   "WHERE u.firstName = :firstName " +
                   "AND u.lastName = :lastName " +
                   "AND u.deleted = false",
           countQuery = "SELECT COUNT(u) FROM UserFile u " +
                        "WHERE u.firstName = :firstName " +
                        "AND u.lastName = :lastName " +
                        "AND u.deleted = false")
    Page<UserFile> findPagedByName(
            @Param("firstName") String firstName,
            @Param("lastName")  String lastName,
            Pageable pageable);

    // ✅ Other existing methods
    @Query("SELECT u FROM UserFile u " +
           "LEFT JOIN FETCH u.fileMetadata " +
           "WHERE u.id = :id AND u.deleted = false")
    Optional<UserFile> findByIdWithMetadata(@Param("id") UUID id);

    @Query("SELECT u FROM UserFile u " +
           "WHERE u.id = :id AND u.deleted = false")
    Optional<UserFile> findActiveById(@Param("id") UUID id);

    List<UserFile> findByLastNameIgnoreCaseAndDeletedFalse(String lastName);
    @Query("SELECT u.keycloakUsername FROM UserFile u WHERE u.id = :fileId AND u.deleted = false")
    String findOwnerUsername(@Param("fileId") UUID fileId);

}