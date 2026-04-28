package com.superme.admin.service;

import com.superme.admin.dto.*;
import com.superme.admin.model.Asset;
import com.superme.admin.model.Folder;
import com.superme.admin.repository.AssetRepository;
import com.superme.admin.repository.FolderRepository;
import com.superme.admin.model.Admin;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssetsAdminService {

    private final FolderRepository folderRepository;
    private final AssetRepository assetRepository;
    private final AuthService adminService;

    // Folder Operations

    @Transactional
    public FolderResponse createFolder(CreateFolderRequest request, Long adminId) {
        Admin admin = adminService.getAdminById(adminId);

        // Check if folder with same name already exists in same location
        if (request.getParentId() == null) {
            if (folderRepository.existsByNameAndParentIsNullAndCreatedBy_Id(request.getName(), adminId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Folder with this name already exists in root");
            }
        } else {
            if (folderRepository.existsByNameAndParentIdAndCreatedBy_Id(
                    request.getName(), request.getParentId(), adminId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Folder with this name already exists in this location");
            }
        }

        Folder parent = null;
        if (request.getParentId() != null) {
            parent = folderRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent folder not found"));

            // Ensure admin owns the parent folder
            if (!parent.getCreatedBy().getId().equals(adminId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have access to this parent folder");
            }
        }

        Folder folder = Folder.builder()
                .name(request.getName())
                .parent(parent)
                .createdBy(admin)
                .build();

        folder = folderRepository.save(folder);
        return mapToFolderResponse(folder);
    }

    @Transactional
    public FolderResponse updateFolder(Long folderId, UpdateFolderRequest request, Long adminId) {
        Folder folder = folderRepository.findByIdAndCreatedBy_Id(folderId, adminId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));

        if (request.getName() != null && !request.getName().isEmpty()) {
            // Check if new name conflicts with existing folder in same location
            Long parentId = folder.getParent() != null ? folder.getParent().getId() : null;
            if (parentId == null) {
                if (folderRepository.existsByNameAndParentIsNullAndCreatedBy_Id(request.getName(), adminId) &&
                        !folder.getName().equals(request.getName())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Folder with this name already exists in root");
                }
            } else {
                if (folderRepository.existsByNameAndParentIdAndCreatedBy_Id(request.getName(), parentId, adminId) &&
                        !folder.getName().equals(request.getName())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Folder with this name already exists in this location");
                }
            }
            folder.setName(request.getName());
        }

        if (request.getParentId() != null) {
            if (request.getParentId().equals(folderId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Folder cannot be its own parent");
            }

            Folder newParent = folderRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "New parent folder not found"));

            // Prevent circular reference
//            if (isCircularReference(folder, newParent)) {
                if (isCircularReference(folder.getId(), newParent.getId())){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Circular reference detected");
            }

            // Ensure admin owns the new parent folder
            if (!newParent.getCreatedBy().getId().equals(adminId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have access to the new parent folder");
            }

            folder.setParent(newParent);
        }

        folder = folderRepository.save(folder);
        return mapToFolderResponse(folder);
    }

    @Transactional
    public void deleteFolder(Long folderId, Long adminId) {
        Folder folder = folderRepository.findByIdAndCreatedBy_Id(folderId, adminId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));

        // Check if folder has assets
        long assetCount = folderRepository.countAssetsInFolder(folderId);
        if (assetCount > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete folder that contains assets. Please delete assets first.");
        }

        // Check if folder has subfolders
        long subFolderCount = folderRepository.countSubFolders(folderId);
        if (subFolderCount > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete folder that contains subfolders. Please delete subfolders first.");
        }

        folderRepository.delete(folder);
    }

    // Asset Operations

    @Transactional
    public AssetResponse createAsset(CreateAssetRequest request, Long adminId) {
        Admin admin = adminService.getAdminById(adminId);

        // Folder is compulsory - no files in root
        if (request.getFolderId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Folder ID is required");
        }

        Folder folder = folderRepository.findById(request.getFolderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));

        // Ensure admin owns the folder
        if (!folder.getCreatedBy().getId().equals(adminId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have access to this folder");
        }

        // Check if asset with same name already exists in folder
        if (assetRepository.existsByFileNameAndFolderIdAndUploadedBy_IdAndDeletedFalse(
                request.getFileName(), request.getFolderId(), adminId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "File with this name already exists in this folder");
        }

        Asset asset = Asset.builder()
                .folder(folder)
                .fileName(request.getFileName())
                .fileUrl(request.getFileUrl())
                .mimeType(request.getMimeType())
                .sizeBytes(request.getSizeBytes())
                .uploadedBy(admin)
                .deleted(false)
                .build();

        asset = assetRepository.save(asset);
        return mapToAssetResponse(asset);
    }

    @Transactional
    public AssetResponse updateAsset(Long assetId, UpdateAssetRequest request, Long adminId) {
        Asset asset = assetRepository.findByIdAndUploadedBy_IdAndDeletedFalse(assetId, adminId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found"));

        if (request.getFileName() != null && !request.getFileName().isEmpty()) {
            // Check if new name conflicts with existing file in same folder
            if (assetRepository.existsByFileNameAndFolderIdAndUploadedBy_IdAndDeletedFalse(
                    request.getFileName(), asset.getFolder().getId(), adminId) &&
                    !asset.getFileName().equals(request.getFileName())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "File with this name already exists in this folder");
            }
            asset.setFileName(request.getFileName());
        }

        if (request.getFolderId() != null && !request.getFolderId().equals(asset.getFolder().getId())) {
            Folder newFolder = folderRepository.findById(request.getFolderId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "New folder not found"));

            // Ensure admin owns the new folder
            if (!newFolder.getCreatedBy().getId().equals(adminId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have access to the new folder");
            }

            asset.setFolder(newFolder);
        }

        asset = assetRepository.save(asset);
        return mapToAssetResponse(asset);
    }

    @Transactional
    public void deleteAsset(Long assetId, Long adminId) {
        Asset asset = assetRepository.findByIdAndUploadedBy_IdAndDeletedFalse(assetId, adminId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found"));

        // Soft delete
        asset.setDeleted(true);
        assetRepository.save(asset);
    }

    @Transactional
    public AssetResponse moveAsset(Long assetId, MoveAssetRequest request, Long adminId) {
        Asset asset = assetRepository.findByIdAndUploadedBy_IdAndDeletedFalse(assetId, adminId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found"));

        Folder targetFolder = folderRepository.findById(request.getTargetFolderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target folder not found"));

        // Ensure admin owns the target folder
        if (!targetFolder.getCreatedBy().getId().equals(adminId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have access to the target folder");
        }

        // Check if asset with same name already exists in target folder
        if (assetRepository.existsByFileNameAndFolderIdAndUploadedBy_IdAndDeletedFalse(
                asset.getFileName(), request.getTargetFolderId(), adminId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "File with this name already exists in the target folder");
        }

        asset.setFolder(targetFolder);
        asset = assetRepository.save(asset);
        return mapToAssetResponse(asset);
    }

    // Get saved assets (folder + assets)
    @Transactional(readOnly = true)
    public SavedAssetsResponse getSavedAssets(Long folderId, Long adminId) {
        Folder currentFolder = null;
        List<Folder> subFolders;
        List<Asset> assets;

        if (folderId == null) {
            // Root folder
            subFolders = folderRepository.findByParentIsNullAndCreatedBy_Id(adminId);
            assets = List.of(); // No assets in root
        } else {
            currentFolder = folderRepository.findByIdAndCreatedBy_Id(folderId, adminId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found"));

            subFolders = folderRepository.findByParentIdAndCreatedBy_Id(folderId, adminId);
            assets = assetRepository.findByFolderIdAndUploadedBy_IdAndDeletedFalse(folderId, adminId);
        }

        return SavedAssetsResponse.builder()
                .currentFolder(currentFolder != null ? mapToFolderResponse(currentFolder) : null)
                .subFolders(subFolders.stream().map(this::mapToFolderResponse).collect(Collectors.toList()))
                .assets(assets.stream().map(this::mapToAssetResponse).collect(Collectors.toList()))
                .build();
    }

    // Helper Methods

//    private boolean isCircularReference(Folder folder, Folder potentialParent) {
//        Folder current = potentialParent;
//        while (current != null) {
//            if (current.getId().equals(folder.getId())) {
//                return true;
//            }
//            current = current.getParent();
//        }
//        return false;
//    }
private boolean isCircularReference(Long folderId, Long parentId) {
    Long currentParentId = parentId;

    while (currentParentId != null) {
        if (currentParentId.equals(folderId)) {
            return true;
        }

        currentParentId = folderRepository.findById(currentParentId)
                .map(f -> f.getParent() != null ? f.getParent().getId() : null)
                .orElse(null);
    }

    return false;
}
    @Transactional(readOnly = true)
    protected FolderResponse mapToFolderResponse(Folder folder) {
        return FolderResponse.builder()
                .id(folder.getId())
                .name(folder.getName())
                .parentId(folder.getParent() != null ? folder.getParent().getId() : null)
//                .parentName(folder.getParent() != null ? folder.getParent().getName() : null)
                .parentName(
                        folder.getParent() != null
                                ? folderRepository.findById(folder.getParent().getId())
                                  .map(Folder::getName)
                                  .orElse(null)
                                : null
                )
                .createdById(folder.getCreatedBy().getId())
//                .createdBy(folder.getCreatedBy().getFullName())
                .createdAt(folder.getCreatedAt().toString())
                .updatedAt(folder.getUpdatedAt() != null ? folder.getUpdatedAt().toString() : null)
                .assetCount(folder.getAssets() != null ? (int) folder.getAssets().stream()
                        .filter(asset -> !asset.isDeleted()).count() : 0)
                .subFolderCount(folder.getSubFolders() != null ? folder.getSubFolders().size() : 0)
                .build();
    }

    private AssetResponse mapToAssetResponse(Asset asset) {
        return AssetResponse.builder()
                .id(asset.getId())
                .fileName(asset.getFileName())
                .fileUrl(asset.getFileUrl())
                .mimeType(asset.getMimeType())
                .sizeBytes(asset.getSizeBytes())
                .formattedSize(formatFileSize(asset.getSizeBytes()))
                .folderId(asset.getFolder().getId())
//                .folderName(asset.getFolder().getName())
                .uploadedById(asset.getUploadedBy().getId())
//                .uploadedBy(asset.getUploadedBy().getFullName())
                .createdAt(asset.getCreatedAt().toString())
                .build();
    }

    private String formatFileSize(Long sizeBytes) {
        if (sizeBytes == null) return "0 B";

        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(sizeBytes) / Math.log10(1024));

        if (digitGroups >= units.length) {
            digitGroups = units.length - 1;
        }

        return new DecimalFormat("#,##0.#").format(sizeBytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
