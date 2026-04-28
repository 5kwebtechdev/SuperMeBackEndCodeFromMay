package com.superme.admin.controller;


import com.superme.admin.dto.*;
import com.superme.admin.service.AssetsAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("admin/assets")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AssetsAdminController {

    private final AssetsAdminService assetsAdminService;

    @PostMapping("/folders")
    public ResponseEntity<ApiResponse<FolderResponse>> createFolder(
            @Valid @RequestBody CreateFolderRequest request,
            @AuthenticationPrincipal Object principal) {

        Long adminId = extractAdminId(principal);
        log.debug("Creating folder for adminId: {}", adminId);

        FolderResponse response = assetsAdminService.createFolder(request, adminId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Folder created successfully", response));
    }

    @PutMapping("/folders/{folderId}")
    public ResponseEntity<ApiResponse<FolderResponse>> updateFolder(
            @PathVariable Long folderId,
            @Valid @RequestBody UpdateFolderRequest request,
            @AuthenticationPrincipal Object principal) {

        Long adminId = extractAdminId(principal);
        log.debug("Updating folder {} for adminId: {}", folderId, adminId);

        FolderResponse response = assetsAdminService.updateFolder(folderId, request, adminId);
        return ResponseEntity.ok(ApiResponse.success("Folder updated successfully", response));
    }

    @DeleteMapping("/folders/{folderId}")
    public ResponseEntity<ApiResponse<Void>> deleteFolder(
            @PathVariable Long folderId,
            @AuthenticationPrincipal Object principal) {

        Long adminId = extractAdminId(principal);
        log.debug("Deleting folder {} for adminId: {}", folderId, adminId);

        assetsAdminService.deleteFolder(folderId, adminId);
        return ResponseEntity.ok(ApiResponse.success("Folder deleted successfully"));
    }

    @PostMapping("/assets")
    public ResponseEntity<ApiResponse<AssetResponse>> createAsset(
            @Valid @RequestBody CreateAssetRequest request,
            @AuthenticationPrincipal Object principal) {

        Long adminId = extractAdminId(principal);
        log.debug("Creating asset for adminId: {}", adminId);

        AssetResponse response = assetsAdminService.createAsset(request, adminId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Asset uploaded successfully", response));
    }

    @PutMapping("/assets/{assetId}")
    public ResponseEntity<ApiResponse<AssetResponse>> updateAsset(
            @PathVariable Long assetId,
            @Valid @RequestBody UpdateAssetRequest request,
            @AuthenticationPrincipal Object principal) {

        Long adminId = extractAdminId(principal);
        log.debug("Updating asset {} for adminId: {}", assetId, adminId);

        AssetResponse response = assetsAdminService.updateAsset(assetId, request, adminId);
        return ResponseEntity.ok(ApiResponse.success("Asset updated successfully", response));
    }

    @DeleteMapping("/assets/{assetId}")
    public ResponseEntity<ApiResponse<Void>> deleteAsset(
            @PathVariable Long assetId,
            @AuthenticationPrincipal Object principal) {

        Long adminId = extractAdminId(principal);
        log.debug("Deleting asset {} for adminId: {}", assetId, adminId);

        assetsAdminService.deleteAsset(assetId, adminId);
        return ResponseEntity.ok(ApiResponse.success("Asset deleted successfully"));
    }

    @PutMapping("/assets/{assetId}/move")
    public ResponseEntity<ApiResponse<AssetResponse>> moveAsset(
            @PathVariable Long assetId,
            @Valid @RequestBody MoveAssetRequest request,
            @AuthenticationPrincipal Object principal) {

        Long adminId = extractAdminId(principal);
        log.debug("Moving asset {} for adminId: {}", assetId, adminId);

        AssetResponse response = assetsAdminService.moveAsset(assetId, request, adminId);
        return ResponseEntity.ok(ApiResponse.success("Asset moved successfully", response));
    }

    @GetMapping("/saved-assets")
    public ResponseEntity<ApiResponse<SavedAssetsResponse>> getSavedAssets(
            @RequestParam(required = false) Long folderId,
            @AuthenticationPrincipal Object principal) {

        Long adminId = extractAdminId(principal);
        log.debug("Getting saved assets for adminId: {}, folderId: {}", adminId, folderId);

        SavedAssetsResponse response = assetsAdminService.getSavedAssets(folderId, adminId);
        return ResponseEntity.ok(ApiResponse.success("Saved assets retrieved successfully", response));
    }

    /**
     * Extract adminId from Spring Security principal
     * The principal can be: Long, String, or UserDetails object
     */
    private Long extractAdminId(Object principal) {
        if (principal == null) {
            log.error("Principal is null - authentication failed");
            throw new org.springframework.security.access.AccessDeniedException("Authentication failed");
        }

        log.debug("Principal type: {}, value: {}",
                principal.getClass().getName(), principal.toString());

        // Case 1: Principal is already Long
        if (principal instanceof Long) {
            return (Long) principal;
        }

        // Case 2: Principal is String (admin ID as string)
        if (principal instanceof String) {
            try {
                return Long.parseLong((String) principal);
            } catch (NumberFormatException e) {
                log.error("Failed to parse adminId from string: {}", principal);
                throw new org.springframework.security.access.AccessDeniedException("Invalid admin ID format");
            }
        }

        // Case 3: Principal is UserDetails
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            String username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            try {
                return Long.parseLong(username);
            } catch (NumberFormatException e) {
                log.error("Failed to parse adminId from UserDetails username: {}", username);
                throw new org.springframework.security.access.AccessDeniedException("Invalid admin ID in UserDetails");
            }
        }

        // Case 4: Try to get adminId via reflection (for custom objects)
        try {
            // Try to find an "id" field or method
            java.lang.reflect.Field idField = principal.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            Object idValue = idField.get(principal);

            if (idValue instanceof Long) {
                return (Long) idValue;
            } else if (idValue instanceof String) {
                return Long.parseLong((String) idValue);
            } else if (idValue instanceof Number) {
                return ((Number) idValue).longValue();
            }
        } catch (Exception e) {
            // Ignore and try other methods
        }

        // Case 5: Try toString() as last resort
        try {
            return Long.parseLong(principal.toString());
        } catch (NumberFormatException e) {
            log.error("Cannot extract adminId from principal: {}", principal);
            throw new org.springframework.security.access.AccessDeniedException("Cannot extract admin ID from principal");
        }
    }
}