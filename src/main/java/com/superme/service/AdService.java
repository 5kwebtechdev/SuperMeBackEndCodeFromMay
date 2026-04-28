package com.superme.service;


import com.superme.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import com.superme.dto.PresignRequestDto;
import com.superme.dto.PresignResponseDto;
import com.superme.dto.AdFilterRequest;
import com.superme.dto.AdRequest;
import com.superme.dto.BannerUploadRequest;
import com.superme.dto.AdResponse;
import com.superme.dto.BannerUploadResponse;
import com.superme.model.Ad;
import com.superme.enums.*;
import com.superme.repository.AdRepository;

import jakarta.persistence.criteria.Predicate;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdService {

    private final AdRepository adRepository;
    private final S3Client s3Client;
    private final S3Presigner presigner;

    @Value("${aws.s3.bucket:superme-ads}")
    private String bucket;

    @Value("${aws.s3.presign-expiry-minutes:30}")
    private int presignExpiryMinutes;

    @Value("${aws.s3.cdn-url:}")
    private String cdnUrl;

    private static final String PUBLIC_BASE_URL =
            "https://mfd-public.s3.ap-south-1.amazonaws.com/";

    // Ad Management Methods
    @Transactional
    public AdResponse createAd(AdRequest adRequest) {
        log.info("Creating new ad with name: {}", adRequest.getAdName());

        if (adRepository.existsByAdName(adRequest.getAdName())) {
            throw new BusinessException("Ad with name '" + adRequest.getAdName() + "' already exists");
        }

        Ad ad = Ad.builder()
                .adName(adRequest.getAdName())
                .adDescription(adRequest.getAdDescription())
                .bannerImageUrl(adRequest.getBannerImageUrl())
                .bannerImageS3Key(adRequest.getBannerImageS3Key())
                .adType(adRequest.getAdType())
                .targetScreen(adRequest.getTargetScreen())
                .targetAudience(adRequest.getTargetAudience())
                .gender(adRequest.getGender())
                .clickActionType(adRequest.getClickActionType())
                .externalUrl(adRequest.getExternalUrl())
                .inAppScreen(adRequest.getInAppScreen())
                .startDateTime(adRequest.getStartDateTime())
                .endDateTime(adRequest.getEndDateTime())
                .noEndDate(adRequest.getNoEndDate())
                .priority(adRequest.getPriority())
                .status(adRequest.getStatus())
                .build();

        Ad savedAd = adRepository.save(ad);
        log.info("Ad created successfully with ID: {}", savedAd.getId());

        return convertToResponse(savedAd);
    }

    public AdResponse getAdById(Long id) {
        log.info("Fetching ad with ID: {}", id);
        Ad ad = adRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Ad not found with ID: " + id));
        return convertToResponse(ad);
    }

    public Page<AdResponse> getAllAds(Pageable pageable) {
        log.info("Fetching all ads with pagination");
        return adRepository.findAll(pageable)
                .map(this::convertToResponse);
    }

    public List<AdResponse> getActiveAds() {
        log.info("Fetching all active ads");
        LocalDateTime now = LocalDateTime.now();
        return adRepository.findByStatusAndStartDateTimeBeforeAndEndDateTimeAfter(
                        AdStatus.ACTIVE, now, now)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<AdResponse> getAdsByFilter(AdFilterRequest filterRequest) {
        log.info("Fetching ads with filters: {}", filterRequest);

        Specification<Ad> spec = buildSpecification(filterRequest);
        return adRepository.findAll(spec)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AdResponse updateAd(Long id, AdRequest adRequest) {
        log.info("Updating ad with ID: {}", id);

        Ad existingAd = adRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Ad not found with ID: " + id));

        if (!existingAd.getAdName().equals(adRequest.getAdName()) &&
                adRepository.existsByAdName(adRequest.getAdName())) {
            throw new BusinessException("Ad with name '" + adRequest.getAdName() + "' already exists");
        }

        existingAd.setAdName(adRequest.getAdName());
        existingAd.setAdDescription(adRequest.getAdDescription());
        existingAd.setBannerImageUrl(adRequest.getBannerImageUrl());
        existingAd.setBannerImageS3Key(adRequest.getBannerImageS3Key());
        existingAd.setAdType(adRequest.getAdType());
        existingAd.setTargetScreen(adRequest.getTargetScreen());
        existingAd.setTargetAudience(adRequest.getTargetAudience());
        existingAd.setGender(adRequest.getGender());
        existingAd.setClickActionType(adRequest.getClickActionType());
        existingAd.setExternalUrl(adRequest.getExternalUrl());
        existingAd.setInAppScreen(adRequest.getInAppScreen());
        existingAd.setStartDateTime(adRequest.getStartDateTime());
        existingAd.setEndDateTime(adRequest.getEndDateTime());
        existingAd.setNoEndDate(adRequest.getNoEndDate());
        existingAd.setPriority(adRequest.getPriority());
        existingAd.setStatus(adRequest.getStatus());

        Ad updatedAd = adRepository.save(existingAd);
        log.info("Ad updated successfully with ID: {}", updatedAd.getId());

        return convertToResponse(updatedAd);
    }

    @Transactional
    public void deleteAd(Long id) {
        log.info("Deleting ad with ID: {}", id);

        Ad ad = adRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Ad not found with ID: " + id));

        // Delete banner image from S3 if exists
        if (ad.getBannerImageS3Key() != null && !ad.getBannerImageS3Key().isEmpty()) {
            deleteBannerImage(ad.getBannerImageS3Key());
        }

        adRepository.deleteById(id);
        log.info("Ad deleted successfully with ID: {}", id);
    }

    @Transactional
    public AdResponse updateAdStatus(Long id, AdStatus status) {
        log.info("Updating status to {} for ad with ID: {}", status, id);

        Ad ad = adRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Ad not found with ID: " + id));

        ad.setStatus(status);
        Ad updatedAd = adRepository.save(ad);

        log.info("Ad status updated successfully for ID: {}", id);
        return convertToResponse(updatedAd);
    }

    public List<AdResponse> getAdsForUser(Integer userAge, Gender userGender, TargetScreen targetScreen) {
        log.info("Fetching ads for user - Age: {}, Gender: {}, Screen: {}", userAge, userGender, targetScreen);

        AgeGroup userAgeGroup = AgeGroup.fromAge(userAge);
        LocalDateTime now = LocalDateTime.now();

        Specification<Ad> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("status"), AdStatus.ACTIVE));
            predicates.add(criteriaBuilder.equal(root.get("targetScreen"), targetScreen));

            Predicate datePredicate = criteriaBuilder.and(
                    criteriaBuilder.lessThanOrEqualTo(root.get("startDateTime"), now),
                    criteriaBuilder.or(
                            criteriaBuilder.isNull(root.get("endDateTime")),
                            criteriaBuilder.greaterThanOrEqualTo(root.get("endDateTime"), now),
                            criteriaBuilder.equal(root.get("noEndDate"), true)
                    )
            );
            predicates.add(datePredicate);

            Predicate agePredicate = criteriaBuilder.or(
                    criteriaBuilder.equal(root.get("targetAudience"), userAgeGroup),
                    criteriaBuilder.equal(root.get("targetAudience"), AgeGroup.ALL)
            );
            predicates.add(agePredicate);

            Predicate genderPredicate = criteriaBuilder.or(
                    criteriaBuilder.equal(root.get("gender"), userGender),
                    criteriaBuilder.equal(root.get("gender"), Gender.ALL)
            );
            predicates.add(genderPredicate);

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return adRepository.findAll(spec)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // File Upload Methods
    public BannerUploadResponse uploadBannerImage(BannerUploadRequest uploadRequest) throws IOException {
        MultipartFile file = uploadRequest.getBannerImage();
        Long userId = uploadRequest.getUserId();

        String s3Key = generateS3Path(userId, file.getOriginalFilename());

        Map<String, String> metadata = new HashMap<>();
        if (userId != null) metadata.put("userId", String.valueOf(userId));
        metadata.put("originalName", file.getOriginalFilename());
        metadata.put("uploadedBy", "ad-service");

        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .contentType(file.getContentType())
                .metadata(metadata)
                .build();

        s3Client.putObject(putReq, software.amazon.awssdk.core.sync.RequestBody.fromInputStream(
                file.getInputStream(), file.getSize()));

        String publicUrl = generatePublicUrl(s3Key);

        return BannerUploadResponse.builder()
                .s3Key(s3Key)
                .publicUrl(publicUrl)
                .status("UPLOADED")
                .message("Banner image uploaded successfully")
                .build();
    }

    public BannerUploadResponse generatePresignedUrlForBanner(Long userId, String fileName, String contentType) {
        String s3Key = generateS3Path(userId, fileName);

        Map<String, String> metadata = new HashMap<>();
        if (userId != null) metadata.put("userId", String.valueOf(userId));
        metadata.put("originalName", fileName);
        metadata.put("uploadedBy", "ad-service");

        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .contentType(contentType)
                .metadata(metadata)
                .build();

        var presigned = presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                        .putObjectRequest(putReq)
                        .signatureDuration(Duration.ofMinutes(presignExpiryMinutes))
                        .build()
        );

        String presignedUrl = presigned.url().toString();
        String publicUrl = generatePublicUrl(s3Key);

        return BannerUploadResponse.builder()
                .s3Key(s3Key)
                .presignedUrl(presignedUrl)
                .publicUrl(publicUrl)
                .status("PRESIGNED")
                .message("Presigned URL generated successfully")
                .build();
    }

    public String getBannerImageUrl(String s3Key) {
        if (s3Key == null || s3Key.isEmpty()) {
            return null;
        }
        return generatePublicUrl(s3Key);
    }

    public boolean deleteBannerImage(String s3Key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .build());
            log.info("Banner image deleted from S3: {}", s3Key);
            return true;
        } catch (S3Exception e) {
            log.error("Failed to delete banner image from S3: {}", s3Key, e);
            return false;
        }
    }

    // Presigned URL Methods using your existing PresignResponseDto
    public PresignResponseDto createPresignedForUser(PresignRequestDto req) {
        String s3Key = generateS3Path(req.getUserId(), req.getOriginalName());
        String fileUrl = PUBLIC_BASE_URL + s3Key;
        Map<String, String> metadata = req.getMetadata() == null ? new HashMap<>() : new HashMap<>(req.getMetadata());
        if (req.getUserId() != null) metadata.put("userId", String.valueOf(req.getUserId()));
        metadata.put("originalName", req.getOriginalName());

        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .contentType(req.getContentType())
                .metadata(metadata)
                .build();

        var presigned = presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                        .putObjectRequest(putReq)
                        .signatureDuration(Duration.ofMinutes(presignExpiryMinutes))
                        .build()
        );

        String url = presigned.url().toString();

        Map<String, String> requiredHeaders = new HashMap<>();
        requiredHeaders.put("Content-Type", req.getContentType());
        metadata.forEach((k, v) -> requiredHeaders.put("x-amz-meta-" + k, v));

        return new PresignResponseDto(null, s3Key, fileUrl, url, "PENDING", requiredHeaders);
    }

    public String generatePresignedGet(String s3Key) {
        GetObjectRequest getReq = GetObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignExpiryMinutes))
                .getObjectRequest(getReq)
                .build();

        return presigner.presignGetObject(presignRequest).url().toString();
    }

    // Utility Methods
    private String generateS3Path(Long userId, String originalName) {
        String uuid = UUID.randomUUID().toString();
        String clean = originalName.replaceAll("\\s+", "_");
        return String.format("ads/banners/%s/%s_%s",
                userId == null ? "anonymous" : userId, uuid, clean);
    }

    private String generatePublicUrl(String s3Key) {
        if (cdnUrl != null && !cdnUrl.isEmpty()) {
            return cdnUrl + "/" + s3Key;
        }
        return String.format("https://%s.s3.amazonaws.com/%s", bucket, s3Key);
    }

    private Specification<Ad> buildSpecification(AdFilterRequest filterRequest) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filterRequest.getTargetScreen() != null) {
                predicates.add(criteriaBuilder.equal(root.get("targetScreen"), filterRequest.getTargetScreen()));
            }

            if (filterRequest.getTargetAudience() != null) {
                predicates.add(criteriaBuilder.equal(root.get("targetAudience"), filterRequest.getTargetAudience()));
            }

            if (filterRequest.getGender() != null) {
                predicates.add(criteriaBuilder.equal(root.get("gender"), filterRequest.getGender()));
            }

            if (filterRequest.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), filterRequest.getStatus()));
            }

            if (filterRequest.getPriority() != null) {
                predicates.add(criteriaBuilder.equal(root.get("priority"), filterRequest.getPriority()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private AdResponse convertToResponse(Ad ad) {
        String bannerUrl = ad.getBannerImageUrl();
        if (bannerUrl == null && ad.getBannerImageS3Key() != null) {
            bannerUrl = getBannerImageUrl(ad.getBannerImageS3Key());
        }

        return AdResponse.builder()
                .id(ad.getId())
                .adName(ad.getAdName())
                .adDescription(ad.getAdDescription())
                .bannerImageUrl(bannerUrl)
                .bannerImageS3Key(ad.getBannerImageS3Key())
                .adType(ad.getAdType())
                .targetScreen(ad.getTargetScreen())
                .targetAudience(ad.getTargetAudience())
                .gender(ad.getGender())
                .clickActionType(ad.getClickActionType())
                .externalUrl(ad.getExternalUrl())
                .inAppScreen(ad.getInAppScreen())
                .startDateTime(ad.getStartDateTime())
                .endDateTime(ad.getEndDateTime())
                .noEndDate(ad.getNoEndDate())
                .priority(ad.getPriority())
                .status(ad.getStatus())
                .createdAt(ad.getCreatedAt())
                .updatedAt(ad.getUpdatedAt())
                .build();
    }
}