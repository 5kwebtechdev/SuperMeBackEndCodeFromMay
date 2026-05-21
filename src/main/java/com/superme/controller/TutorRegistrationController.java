package com.superme.controller;

import com.superme.service.TutorRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
public class TutorRegistrationController {

    private final TutorRegistrationService tutorRegistrationService;

    @PostMapping(value = "/request-tutor-register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> requestTutorRegister(
            @RequestParam Map<String, String> formParams,
            @RequestParam(value = "profile_pic", required = false) MultipartFile profilePic,
            @RequestParam(value = "documents[]", required = false) List<MultipartFile> documents) {
        tutorRegistrationService.sendTutorRegistrationEmail(formParams, profilePic, documents);
        return ResponseEntity.ok(Map.of("message", "Registration request submitted successfully."));
    }
}