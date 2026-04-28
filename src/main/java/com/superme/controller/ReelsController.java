package com.superme.controller;

import com.superme.exception.InternalServerErrorException;
import com.superme.exception.ResourceNotFoundException;
import com.superme.model.Reels;
import com.superme.service.ReelsService;
import com.superme.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.superme.dto.ReelCreateRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reels")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.DELETE, RequestMethod.OPTIONS })
public class ReelsController {
    @Autowired
    private ReelsService reelsService;

    @Autowired
    private S3Service s3Service;

    /**
     * Endpoint to generate a pre-signed S3 upload URL for the client.
     * Client should call this first, upload the video using the returned uploadUrl,
     * then use the returned fileUrl as videoUrl in the POST /reels endpoint.
     */
//    @GetMapping("/upload-url")
//    public ResponseEntity<S3Service.PresignedUploadResponse> getUploadUrl(@RequestParam String filename) {
//        S3Service.PresignedUploadResponse response = s3Service.generatePresignedUploadUrl(filename, 10); // 10 minutes
//                                                                                                         // validity
//        return ResponseEntity.ok(response);
//    }

    @PostMapping
    public ResponseEntity<?> createReel(@RequestBody ReelCreateRequest request) {
        try {
            if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
                return ResponseEntity.status(400).body(Map.of("error", "Title is required"));
            }
            Reels reel = new Reels();
            reel.setTitle(request.getTitle());
            if (request.getHashtags() != null)
                reel.setHashtags(request.getHashtags());
            if (request.getCategory() != null)
                reel.setCategory(request.getCategory());
            if (request.getDescription() != null)
                reel.setDescription(request.getDescription());
            if (request.getDuration() != null)
                reel.setDuration(request.getDuration());
            if (request.getVideoUrl() != null)
                reel.setVideoUrl(request.getVideoUrl());
            if (request.getAiLabel() != null)
                reel.setAiLabel(request.getAiLabel());
            if (request.getCreatedBy() != null)
                reel.setCreatedBy(request.getCreatedBy());
            if (request.getCreatedAt() != null) {
                // Convert epoch millis to LocalDate and LocalTime
                java.time.Instant instant = java.time.Instant.ofEpochMilli(request.getCreatedAt());
                java.time.LocalDateTime ldt = java.time.LocalDateTime.ofInstant(instant,
                        java.time.ZoneId.systemDefault());
                reel.setCreatedDate(ldt.toLocalDate());
                reel.setCreatedTime(ldt.toLocalTime());
            }
            Reels saved = reelsService.createReel(reel);
            return ResponseEntity.status(201).body(saved);
        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to create reel");
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllReels() {
        try {
            List<Reels> reels = reelsService.getAllReels();
            return ResponseEntity.ok(reels);
        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to fetch reel");        }
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<?> likeReel(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        try {
            Long userId = body.get("userId");
            int likes = reelsService.likeReel(id, userId);
            if (likes == -1) {
                throw new ResourceNotFoundException("Reel not found");
            }
            return ResponseEntity.ok(Map.of("likes", likes));
        } catch (Exception e) {
           throw new InternalServerErrorException("Failed to like reel");
        }
    }

    @PostMapping("/{id}/save")
    public ResponseEntity<?> saveReel(@PathVariable Long id, @RequestBody Map<String, Long> body) {
        try {
            Long userId = body.get("userId");
            reelsService.saveReel(id, userId);
            return ResponseEntity.ok(Map.of("message", "Reel saved"));
        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to save reel");        }
    }

    @PostMapping("/{id}/share")
    public ResponseEntity<?> shareReel(@PathVariable Long id) {
        try {

            int shares = reelsService.shareReel(id);
            if (shares == -1) {
                throw new ResourceNotFoundException("Reel not found");
            }
            return ResponseEntity.ok(Map.of("shares", shares));
        } catch (Exception e) {
            throw new InternalServerErrorException("Failed to share reel");
        }
    }

    /**
     * Endpoint to generate a pre-signed S3 upload URL for the client.
     * Client should call this first, upload the video using the returned uploadUrl,
     * then use the returned fileUrl as videoUrl in the POST /reels endpoint.
     */

}
