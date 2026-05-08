package com.superme.controller;

import com.superme.config.FileStorageConfig;
import com.superme.dto.LikeResponse;
import com.superme.dto.TutorsResponse;
import com.superme.util.UserJwtUtil;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;
import com.superme.model.Tutor;
import com.superme.service.TutorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tutors")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class TutorController {

    @Autowired
    private TutorService tutorService;

//    @GetMapping
//    public List<TutorsResponse> getTutors(@RequestHeader("Authorization") String token,
//            @RequestParam(required = false) String subject,
//            @RequestParam(required = false) String mode,
//            @RequestParam(required = false) String standard,
//            @RequestParam(required = false) String location,
//            @RequestParam(required = false) String startTime,
//            @RequestParam(required = false) String endTime,
//            @RequestParam(required = false) List<String> levels) {
//
//        Long userId = UserJwtUtil.getUserIdFromToken(
//                token.replace("Bearer ", "")
//        );
//
//        java.time.LocalDateTime start = startTime != null ? java.time.LocalDateTime.parse(startTime) : null;
//        java.time.LocalDateTime end = endTime != null ? java.time.LocalDateTime.parse(endTime) : null;
//
//        if (start != null && end != null && levels != null && !levels.isEmpty()) {
//            return tutorService.getTutorsByTimeAndLevels(userId,start, end, levels);
//        } else if (start != null && end != null) {
//            return tutorService.getTutorsByTimeRange(userId,start, end);
//        } else if (levels != null && !levels.isEmpty()) {
//            return tutorService.getTutorsByLevels(userId,levels);
//        } else {
//            return tutorService.getTutorsWithFilters(userId,subject, mode, standard, location);
//        }
//    }




    @GetMapping
    public List<TutorsResponse> getTutors(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) String standard,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) List<String> levels) {

        Long userId = UserJwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));

        LocalDateTime start = startTime != null ? LocalDateTime.parse(startTime) : null;
        LocalDateTime end = endTime != null ? LocalDateTime.parse(endTime) : null;

        return tutorService.getTutors(
                userId, subject, mode, standard, location, start, end, levels
        );
    }




    @GetMapping("/{id}")
    public Tutor getTutorById(@PathVariable Long id) {
        return tutorService.getTutorById(id);
    }

    @PostMapping("/like")
    public ResponseEntity<?> likeTutor(
            @RequestHeader("Authorization") String token,
            @RequestParam Long tutorId) {

        Long userId = UserJwtUtil.getUserIdFromToken(
                token.replace("Bearer ", "")
        );
        LikeResponse response = tutorService.likeTutor(tutorId, userId);
        return ResponseEntity.ok(Map.of(
                "message", response.isLiked()
                        ? "Tutor liked successfully."
                        : "Tutor unliked successfully.",
                "liked", response.isLiked(),
                "champsLiked", response.getTotalLikes()
        ));
    }


    // New: Get tutors by time range
    @GetMapping("/by-time")
    public List<TutorsResponse> getTutorsByTimeRange(
            @RequestHeader("Authorization") String token,
            @RequestParam("startTime") String startTime,
            @RequestParam("endTime") String endTime) {

        Long userId = UserJwtUtil.getUserIdFromToken(
                token.replace("Bearer ", "")
        );

        java.time.LocalDateTime start = java.time.LocalDateTime.parse(startTime);
        java.time.LocalDateTime end = java.time.LocalDateTime.parse(endTime);
        return tutorService.getTutorsByTimeRange(userId, start, end);
    }

    // New: Get tutors by a single level
    @GetMapping("/by-level")
    public List<Tutor> getTutorsByLevel(@RequestParam("level") String level) {
        return tutorService.getTutorsByLevel(level);
    }

    // New: Get tutors by multiple levels
    @GetMapping("/by-levels")
    public List<TutorsResponse> getTutorsByLevels( @RequestHeader("Authorization") String token,@RequestParam("levels") List<String> levels) {
        Long userId = UserJwtUtil.getUserIdFromToken(
                token.replace("Bearer ", "")
        );
        return tutorService.getTutorsByLevels(userId, levels);
    }

    // New: Get tutors by time range and levels (combined filter)
    @GetMapping("/by-time-and-levels")
    public List<TutorsResponse> getTutorsByTimeAndLevels(
            @RequestHeader("Authorization") String token,
            @RequestParam("startTime") String startTime,
            @RequestParam("endTime") String endTime,
            @RequestParam("levels") List<String> levels) {
        Long userId = UserJwtUtil.getUserIdFromToken(
                token.replace("Bearer ", "")
        );

        java.time.LocalDateTime start = java.time.LocalDateTime.parse(startTime);
        java.time.LocalDateTime end = java.time.LocalDateTime.parse(endTime);
        return tutorService.getTutorsByTimeAndLevels(userId, start, end, levels);
    }



    @Autowired
    private FileStorageConfig fileStorageConfig;

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {

        try {
            Path filePath = Paths.get(fileStorageConfig.getUploadDir())
                    .resolve(fileName)
                    .normalize();

            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                throw new RuntimeException("File not found");
            }

            String contentType = "image/png"; // default

            // optional: detect type dynamically
            try {
                contentType = Files.probeContentType(filePath);
            } catch (Exception ignored) {}

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            throw new RuntimeException("Error reading file", e);
        }
    }



}