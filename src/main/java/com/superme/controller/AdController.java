package com.superme.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.superme.dto.PresignRequestDto;
import com.superme.dto.PresignResponseDto;
import com.superme.dto.AdFilterRequest;
import com.superme.dto.AdRequest;
import com.superme.dto.BannerUploadRequest;
import com.superme.dto.AdResponse;
import com.superme.dto.BannerUploadResponse;
import com.superme.enums.AdStatus;
import com.superme.enums.Gender;
import com.superme.enums.TargetScreen;
import com.superme.service.AdService;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;

/**
 * REST Controller for managing advertisements and banner uploads
 * Provides endpoints for CRUD operations, filtering, and file uploads
 */
@RestController
@RequestMapping("/ads")
@RequiredArgsConstructor
public class AdController {

    private final AdService adService;

    /**
     * CREATE - Create a new advertisement
     * Handles the creation of new ads with all necessary metadata
     *
     * @param adRequest DTO containing ad details (name, description, target audience, etc.)
     * @return AdResponse with created ad details and generated ID
     */
    @PostMapping
    public ResponseEntity<AdResponse> createAd(@Valid @RequestBody AdRequest adRequest) {
        AdResponse response = adService.createAd(adRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * READ - Get paginated list of all advertisements
     * Supports sorting and pagination for efficient data retrieval
     *
     * @param page Page number (default: 0)
     * @param size Page size (default: 10)
     * @param sortBy Field to sort by (default: createdAt)
     * @param direction Sort direction ASC/DESC (default: DESC)
     * @return Paginated list of AdResponse objects
     */
    @GetMapping
    public ResponseEntity<Page<AdResponse>> getAllAds(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {

        Sort sort = direction.equalsIgnoreCase("ASC") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<AdResponse> response = adService.getAllAds(pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * READ - Get targeted ads for specific users
     * Filters ads based on user demographics and current screen context
     * Used by frontend to display relevant ads to users
     *
     * @param age User's age for age-group targeting
     * @param gender User's gender for gender targeting
     * @param targetScreen Current screen/context where ad will be displayed
     * @return List of targeted AdResponse objects
     */
    @GetMapping("/user-targeted")
    public ResponseEntity<List<AdResponse>> getAdsForUser(
            @RequestParam Integer age,
            @RequestParam Gender gender,
            @RequestParam TargetScreen targetScreen) {
        List<AdResponse> response = adService.getAdsForUser(age, gender, targetScreen);
        return ResponseEntity.ok(response);
    }

    /**
     * UPDATE - Update advertisement status
     * Used to activate, pause, or change ad status
     *
     * @param id Advertisement ID to update
     * @param status New status (ACTIVE, PAUSED, DRAFT, EXPIRED)
     * @return Updated AdResponse object
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<AdResponse> updateAdStatus(@PathVariable Long id, @RequestParam AdStatus status) {
        AdResponse response = adService.updateAdStatus(id, status);
        return ResponseEntity.ok(response);
    }

    /**
     * FILE UPLOAD - Generate presigned URL for banner image upload
     * Provides secure, temporary upload URL for client-side direct S3 uploads
     *
     * @param fileName Original filename of the banner image
     * @param contentType MIME type of the image (e.g., image/jpeg, image/png)
     * @param userId Optional user ID for tracking uploads
     * @return BannerUploadResponse with presigned URL and S3 key
     */
    @GetMapping("/banner/presigned-url")
    public ResponseEntity<BannerUploadResponse> generatePresignedUrl(
            @RequestParam String fileName,
            @RequestParam String contentType,
            @RequestParam(value = "userId", required = false) Long userId) {

        BannerUploadResponse response = adService.generatePresignedUrlForBanner(userId, fileName, contentType);
        return ResponseEntity.ok(response);
    }
}