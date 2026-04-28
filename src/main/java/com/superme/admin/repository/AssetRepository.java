package com.superme.admin.repository;


import com.superme.admin.model.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    List<Asset> findByFolderIdAndDeletedFalse(Long folderId);

    List<Asset> findByFolderIdAndUploadedBy_IdAndDeletedFalse(Long folderId, Long uploadedById);

    Optional<Asset> findByIdAndUploadedBy_IdAndDeletedFalse(Long id, Long uploadedById);

    boolean existsByFileNameAndFolderIdAndUploadedBy_IdAndDeletedFalse(
            String fileName, Long folderId, Long uploadedById);
}
