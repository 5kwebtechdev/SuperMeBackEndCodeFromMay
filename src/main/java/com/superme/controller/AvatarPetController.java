package com.superme.controller;

import com.superme.dto.AvatarPetRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Returns all available avatars and pets with their image URLs and names.
 * The mobile app can use this to render selection grids.
 */
@RestController
@RequestMapping("/options")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.DELETE, RequestMethod.OPTIONS })
public class AvatarPetController {

    private static final String S3_BASE_URL = "https://your-bucket.s3.amazonaws.com/";

    @GetMapping("/avatars")
    public List<Map<String, String>> getAvatars() {
        return List.of(
                icon("avatar1", "avatars/avatar1.png"),
                icon("avatar2", "avatars/avatar2.png"),
                icon("avatar3", "avatars/avatar3.png"),
                icon("avatar4", "avatars/avatar4.png"));
    }

    @GetMapping("/pets")
    public List<Map<String, String>> getPets() {
        return List.of(
                icon("pet1", "pets/pet1.png"),
                icon("pet2", "pets/pet2.png"),
                icon("pet3", "pets/pet3.png"),
                icon("pet4", "pets/pet4.png"));
    }

    private Map<String, String> icon(String name, String path) {
        return Map.of(
                "name", name,
                "imageUrl", S3_BASE_URL + path);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/options/avatars/add")
    public ResponseEntity<String> addAvatar(@RequestBody AvatarPetRequest req) {
        // Add avatar logic (in-memory or DB)
        return ResponseEntity.ok("Avatar added");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/options/avatars/update")
    public ResponseEntity<String> updateAvatar(@RequestBody AvatarPetRequest req) {
        // Update avatar logic
        return ResponseEntity.ok("Avatar updated");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/options/avatars/delete")
    public ResponseEntity<String> deleteAvatar(@RequestBody AvatarPetRequest req) {
        // Delete avatar logic
        return ResponseEntity.ok("Avatar deleted");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/options/pets/add")
    public ResponseEntity<String> addPet(@RequestBody AvatarPetRequest req) {
        // Add pet logic
        return ResponseEntity.ok("Pet added");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/options/pets/update")
    public ResponseEntity<String> updatePet(@RequestBody AvatarPetRequest req) {
        // Update pet logic
        return ResponseEntity.ok("Pet updated");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/options/pets/delete")
    public ResponseEntity<String> deletePet(@RequestBody AvatarPetRequest req) {
        // Delete pet logic
        return ResponseEntity.ok("Pet deleted");
    }
}