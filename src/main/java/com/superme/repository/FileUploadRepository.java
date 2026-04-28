package com.superme.repository;

import com.superme.model.FileUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileUploadRepository extends JpaRepository<FileUpload, Long> {
    Optional<FileUpload> findByFileName(String fileName);
    List<FileUpload> findByUserId(Long userId);

    Optional<FileUpload> findByUserIdAndOriginalName(Long userId, String originalName);

    Optional<FileUpload> findByAdminIdAndOriginalName(Long adminId, String originalName);

    List<FileUpload> findByAdminId(Long id);
}
