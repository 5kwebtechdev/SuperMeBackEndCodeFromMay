package com.superme.controller;

import com.superme.dto.PresignRequestDto;
import com.superme.dto.PresignResponseDto;
import com.superme.model.FileUpload;
import com.superme.service.S3Service;
import com.superme.util.UserJwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/s3")
@RequiredArgsConstructor
public class S3Controller {

    private final S3Service s3Service;

    // 1. Generate single presigned URL (POST)
    @PostMapping("/presign")
    public PresignResponseDto presign(@RequestHeader("Authorization") String token,@RequestBody PresignRequestDto dto) {
        Long userId = UserJwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        String role = UserJwtUtil.getRoleFromToken(token.replace("Bearer ", ""));
        dto.setUserId(userId);
        dto.setRole(role);
        // server-side validation for expected size
        if (dto.getFileSize() != null) {
            long maxBytes = (long) (dto.getFileSize() / (1024.0 * 1024.0)); // not exact — better to check value
            // We use upload.max-size-mb in service for enforcement on client
        }
        return s3Service.createPresignedForUser(dto);
    }

    // 2. Generate multiple presigned URLs
    @PostMapping("/presign/multiple")
    public List<PresignResponseDto> presignMultiple(
            @RequestHeader("Authorization") String token,
            @RequestBody List<PresignRequestDto> dtos) {
        Long userId = UserJwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        String role = UserJwtUtil.getRoleFromToken(token.replace("Bearer ", ""));
        return s3Service.createMultiplePresigned(userId,role, dtos);
    }

    // 3. Upload complete callback (frontend calls after successful PUT)
    @PostMapping("/upload-complete/{fileId}")
    public ResponseEntity<String> uploadComplete(@PathVariable Long fileId) {
        boolean ok = s3Service.verifyAndMarkUploaded(fileId);
        return ok ? ResponseEntity.ok("OK") : ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Upload not found");
    }

    // 4. Generate presigned GET for download
    @GetMapping("/download")
    public Map<String, String> download(@RequestParam String s3Key) {
        String url = s3Service.generatePresignedGet(s3Key);
        return Map.of("url", url);
    }

    // 5. Delete file
    @DeleteMapping("/delete")
    public Map<String, Object> delete(@RequestParam String s3Key) {
        boolean deleted = s3Service.deleteObject(s3Key);
        return Map.of("deleted", deleted);
    }

    // 6. List objects by prefix (e.g., "uploads/{userId}")
    @GetMapping("/list")
    public List<String> list(@RequestParam(required = false) String prefix) {
        return s3Service.listObjects(prefix);
    }

    // 7. List DB records for a user
    @GetMapping("/files")
    public List<FileUpload> filesByUser(@RequestHeader("Authorization") String token) {
        Long userId = UserJwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        String role = UserJwtUtil.getRoleFromToken(token.replace("Bearer ", ""));
        return s3Service.getFilesByUser(userId,role);
    }

}


