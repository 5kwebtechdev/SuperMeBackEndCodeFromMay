package com.superme.admin.controller;


import com.superme.admin.dto.ParentUserRequestDTO;
import com.superme.admin.dto.ParentUserResponseDTO;
import com.superme.admin.dto.ParentWithChildrenResponseDTO;
import com.superme.admin.service.ParentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/parent-users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Parent User Management", description = "APIs for managing parent users")
public class ParentUserController {
    
    private final ParentUserService parentUserService;
    
    /**
     * Create a new parent user
     * POST /api/v1/admin/parent-users
     */
    @PostMapping
    @Operation(summary = "Create a new parent user")
    public ResponseEntity<ParentUserResponseDTO> createParentUser(@Valid @RequestBody ParentUserRequestDTO request) {
        log.info("REST request to create parent user: {}", request);
        ParentUserResponseDTO response = parentUserService.createParentUser(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    /**
     * Update an existing parent user
     * PUT /api/v1/admin/parent-users/{id}
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update an existing parent user")
    public ResponseEntity<ParentUserResponseDTO> updateParentUser(
            @PathVariable Long id,
            @Valid @RequestBody ParentUserRequestDTO request) {
        log.info("REST request to update parent user with ID: {}", id);
        request.setId(id);
        ParentUserResponseDTO response = parentUserService.updateParentUser(id, request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get parent user by ID
     * GET /api/v1/admin/parent-users/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get parent user by ID")
    public ResponseEntity<ParentUserResponseDTO> getParentUserById(@PathVariable Long id) {
        log.info("REST request to get parent user with ID: {}", id);
        ParentUserResponseDTO response = parentUserService.getParentUserById(id);
        return ResponseEntity.ok(response);
    }
    

    
    /**
     * Get all parent users (list without pagination)
     * GET /api/v1/admin/parent-users/all
     */
    @GetMapping("/all")
    @Operation(summary = "Get all parent users (list format)")
    public ResponseEntity<List<ParentUserResponseDTO>> getAllParentUsersList() {
        log.info("REST request to get all parent users as list");
        List<ParentUserResponseDTO> response = parentUserService.getAllParentUsersList();
        return ResponseEntity.ok(response);
    }
    
    /**
     * Delete parent user (soft delete)
     * DELETE /api/v1/admin/parent-users/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete parent user")
    public ResponseEntity<Void> deleteParentUser(@PathVariable Long id) {
        log.info("REST request to delete parent user with ID: {}", id);
        parentUserService.deleteParentUser(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Enable/Disable parent user
     * PATCH /api/v1/admin/parent-users/{id}/status?enabled=true
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "Enable or disable parent user")
    public ResponseEntity<ParentUserResponseDTO> setUserStatus(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        log.info("REST request to set status for user ID: {} to enabled: {}", id, enabled);
        ParentUserResponseDTO response = parentUserService.setUserStatus(id, enabled);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Check if family code exists
     * GET /api/v1/admin/parent-users/check-family-code/{code}
     */
    @GetMapping("/check-family-code/{code}")
    @Operation(summary = "Check if family code exists")
    public ResponseEntity<Boolean> checkFamilyCodeExists(@PathVariable String code) {
        log.info("REST request to check family code: {}", code);
        boolean exists = parentUserService.checkFamilyCodeExists(code);
        return ResponseEntity.ok(exists);
    }

    /**
     * Get parent user along with their children
     * GET /v1/admin/parent-users/parent-with-child/{id}
     */
    @GetMapping("/parent-with-child/{id}")
    @Operation(summary = "Get parent user with their children")
    public ResponseEntity<?> getParentWithChildren(@PathVariable Long id) {
        log.info("REST request to get parent with children for ID: {}", id);
        try {
            ParentWithChildrenResponseDTO response = parentUserService.getParentWithChildren(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /v1/admin/parent-users/download-excel
     *
     * Downloads a filtered Excel of all parent users.
     * Each row includes full parent details, complete family info, and children count.
     *
     * Optional query params:
     *   q             – keyword (name / email / phone)
     *   gender        – MALE | FEMALE
     *   enabled       – true | false
     *   ageGroup      – BELOW_11 | AGE_11_TO_13 | AGE_14_TO_15 | AGE_16_TO_17 | AGE_18_PLUS
     *   minAge        – minimum age (inclusive)
     *   maxAge        – maximum age (inclusive)
     *   emailVerified – true | false
     *   familyCode    – filter by exact family code
     *   hasChildren   – true = parents with ≥1 child | false = parents with no children
     *   createdFrom   – yyyy-MM-dd
     *   createdTo     – yyyy-MM-dd
     */
    @GetMapping("/download-excel")
    @Operation(summary = "Download filtered parent users as Excel")
    public ResponseEntity<?> downloadParentUsersExcel(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String ageGroup,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) Boolean emailVerified,
            @RequestParam(required = false) String familyCode,
            @RequestParam(required = false) Boolean hasChildren,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo) {
        log.info("REST request to download parent users Excel");
        try {
            byte[] excel = parentUserService.generateParentUsersExcel(
                    q, gender, enabled, ageGroup, minAge, maxAge,
                    emailVerified, familyCode, hasChildren, createdFrom, createdTo);

            String filename = "parent-users-" + LocalDate.now() + ".xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excel);
        } catch (IOException e) {
            log.error("Parent users Excel generation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate Excel: " + e.getMessage()));
        }
    }
}