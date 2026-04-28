package com.superme.admin.repository;

import com.superme.admin.model.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {
    @Query("SELECT f.name FROM Folder f WHERE f.id = :id")
    String findFolderNameById(@Param("id") Long id);
    Optional<Folder> findByIdAndCreatedBy_Id(Long id, Long createdById);

    List<Folder> findByParentIdAndCreatedBy_Id(Long parentId, Long createdById);

    List<Folder> findByParentIsNullAndCreatedBy_Id(Long createdById);

    boolean existsByNameAndParentIdAndCreatedBy_Id(String name, Long parentId, Long createdById);

    boolean existsByNameAndParentIsNullAndCreatedBy_Id(String name, Long createdById);

    @Query("SELECT COUNT(a) FROM Asset a WHERE a.folder.id = :folderId AND a.deleted = false")
    long countAssetsInFolder(@Param("folderId") Long folderId);

    @Query("SELECT COUNT(f) FROM Folder f WHERE f.parent.id = :folderId")
    long countSubFolders(@Param("folderId") Long folderId);
}