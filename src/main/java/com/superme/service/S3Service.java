package com.superme.service;

import com.superme.admin.security.AdminJwtUtil;
import com.superme.dto.PresignRequestDto;
import com.superme.dto.PresignResponseDto;
import com.superme.model.FileUpload;
import com.superme.repository.FileUploadRepository;
import com.superme.util.UserJwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final FileUploadRepository fileRepo;

    @Value("${s3.bucket}")
    private String bucket;

    @Value("${s3.presign.expiry-minutes:10}")
    private long presignExpiryMinutes;

    @Value("${upload.max-size-mb:20}")
    private long uploadMaxSizeMb;

    private static final String PUBLIC_BASE_URL =
            "https://mfd-public.s3.ap-south-1.amazonaws.com/";

    private String generateS3Path(Long userId, Long adminId, String originalName) {
        String uuid = UUID.randomUUID().toString();
        String clean = originalName.replaceAll("\\s+", "_");

        String folder;
        if (adminId != null) {
            folder = String.format("uploads/admin/%s", adminId);
        } else if (userId != null) {
            folder = String.format("uploads/user/%s", userId);
        } else {
            folder = "uploads/anonymous";
        }

        return String.format("%s/%s_%s", folder, uuid, clean);
    }


    // SINGLE presigned PUT + DB record
    public PresignResponseDto createPresignedForUser(PresignRequestDto req) {

        Long userId;
        Long adminId;
        String s3Key;

        if ("ADMIN".equals(req.getRole())) {
            userId = null;
            adminId = req.getAdminId(); // Reuse same method for admin ID
            s3Key = generateS3Path(null,adminId, req.getOriginalName());

        } else {
            adminId = null;
            // For USER role, get from request or token
            userId = req.getUserId();
            s3Key = generateS3Path(userId,null, req.getOriginalName());
        }



        // Prepare metadata
        Map<String, String> metadata = req.getMetadata() == null
                ? new HashMap<>()
                : new HashMap<>(req.getMetadata());

        // Set appropriate ID based on role
        if (userId != null) {
            metadata.put("userId", String.valueOf(userId));
        } else if (adminId != null) {
            metadata.put("adminId", String.valueOf(adminId));
        }
        metadata.put("originalName", req.getOriginalName());
        metadata.put("role", req.getRole());

        // Check if file already exists - adjust query based on role
        Optional<FileUpload> existingOpt;
        if ("ADMIN".equals(req.getRole())) {
            existingOpt = fileRepo.findByAdminIdAndOriginalName(adminId, req.getOriginalName());
        } else {
            existingOpt = fileRepo.findByUserIdAndOriginalName(userId, req.getOriginalName());
        }

        FileUpload file;

        // Reuse existing record or create new
        file = existingOpt.orElseGet(() -> FileUpload.builder()
                .userId(userId)
                .adminId(adminId)
                .fileName(s3Key)
                .fileUrl(PUBLIC_BASE_URL + s3Key)
                .originalName(req.getOriginalName())
                .contentType(req.getContentType())
                .fileSize(req.getFileSize())
                .metadata(metadata)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build());

        // Generate presigned request
        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .contentType(req.getContentType())
                .metadata(metadata)
                .build();

        PresignedPutObjectRequest presigned = presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                        .putObjectRequest(putReq)
                        .signatureDuration(Duration.ofMinutes(presignExpiryMinutes))
                        .build()
        );

        String url = presigned.url().toString();

        // Update file with new URL
        file.setPresignedUrl(url);
        file.setUpdatedAt(LocalDateTime.now());
        file.setMetadata(metadata);
        file.setFileUrl(PUBLIC_BASE_URL + s3Key);

        file = fileRepo.save(file);

        // Prepare required headers
        Map<String, String> requiredHeaders = new HashMap<>();
        requiredHeaders.put("Content-Type", req.getContentType());
        metadata.forEach((k, v) -> requiredHeaders.put("x-amz-meta-" + k, v));

        return new PresignResponseDto(file.getId(), s3Key, file.getFileUrl(), url, file.getStatus(), requiredHeaders);
    }

    // MULTIPLE presigned URLs for a list of originals
    public List<PresignResponseDto> createMultiplePresigned(Long id, String role, List<PresignRequestDto> requests) {
        List<PresignResponseDto> res = new ArrayList<>();
        Long adminId = null;

        Long userId = null;
        if ("ADMIN".equals(role)) {
            adminId = id;
        } else {
            userId = id;
        }

        for (PresignRequestDto r : requests) {
            if ("ADMIN".equals(role)) {
                r.setAdminId(adminId);  // Assuming you added adminId to PresignRequestDto
                r.setUserId(null);
                r.setRole("ADMIN");
            } else {
                r.setUserId(userId);
                r.setAdminId(null);
                r.setRole("USER");
            }
            res.add(createPresignedForUser(r));
        }
        return res;
    }


    // After upload: verify via HEAD and mark uploaded
    public boolean verifyAndMarkUploaded(Long fileId) {
        FileUpload file = fileRepo.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        try {
            HeadObjectResponse head = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(file.getFileName())
                    .build());

            long size = head.contentLength();
            file.setFileSize(size);
            file.setStatus("UPLOADED");
            fileRepo.save(file);
            return true;
        } catch (S3Exception e) {
            file.setStatus("FAILED");
            fileRepo.save(file);
            return false;
        }
    }

    // Generate GET presigned URL for download
    public String generatePresignedGet(String s3Key) {
        GetObjectRequest getReq = GetObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofDays(7))
                .getObjectRequest(getReq)
                .build();

        return presigner.presignGetObject(presignRequest).url().toString();
    }

    // Delete object (and optionally DB entry)
    public boolean deleteObject(String s3Key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .build());
            // optionally delete DB record if exists
            fileRepo.findByFileName(s3Key).ifPresent(fileRepo::delete);
            return true;
        } catch (S3Exception e) {
            return false;
        }
    }

    // List objects under a prefix (e.g., user's folder)
    public List<String> listObjects(String prefix) {
        ListObjectsV2Request.Builder req = ListObjectsV2Request.builder().bucket(bucket);
        if (prefix != null && !prefix.isBlank()) req.prefix(prefix.endsWith("/") ? prefix : prefix + "/");
        ListObjectsV2Response res = s3Client.listObjectsV2(req.build());
        return res.contents().stream().map(S3Object::key).collect(Collectors.toList());
    }

    // Get all files by user from DB
    public List<FileUpload> getFilesByUser(Long id,String role) {
        if ("ADMIN".equals(role)) {
            return fileRepo.findByAdminId(id);
        } else {
            return fileRepo.findByUserId(id);
        }

    }

}


