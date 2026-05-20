package com.superme.admin.controller;


import com.superme.admin.dto.ChildUserRequestDTO;
import com.superme.admin.dto.ChildUserResponseDTO;
import com.superme.admin.service.ChildUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
@RequestMapping("/admin/children")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Child User Management", description = "APIs for managing child users")
public class ChildUserController {
    
    private final ChildUserService childUserService;
    
    /**
     * Create a new child user
     * POST /v1/admin/children
     */
    @PostMapping
    @Operation(summary = "Create a new child user")
    public ResponseEntity<ChildUserResponseDTO> createChildUser(@Valid @RequestBody ChildUserRequestDTO request) {
        log.info("REST request to create child user: {}", request);
        ChildUserResponseDTO response = childUserService.createChildUser(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    /**
     * Update an existing child user
     * PUT /v1/admin/children/{id}
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update an existing child user")
    public ResponseEntity<ChildUserResponseDTO> updateChildUser(
            @PathVariable Long id,
            @Valid @RequestBody ChildUserRequestDTO request) {
        log.info("REST request to update child user with ID: {}", id);
        request.setId(id);
        ChildUserResponseDTO response = childUserService.updateChildUser(id, request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get child user by ID
     * GET /v1/admin/children/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get child user by ID")
    public ResponseEntity<ChildUserResponseDTO> getChildUserById(@PathVariable Long id) {
        log.info("REST request to get child user with ID: {}", id);
        ChildUserResponseDTO response = childUserService.getChildUserById(id);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all child users with pagination
     * GET /v1/admin/children?page=0&size=10&search=keyword
     */
    @GetMapping
    @Operation(summary = "Get all child users with pagination")
    public ResponseEntity<Page<ChildUserResponseDTO>> getAllChildUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        log.info("REST request to get all child users - page: {}, size: {}, search: {}", page, size, search);
        
        Sort sort = sortDir.equalsIgnoreCase("asc") ? 
                    Sort.by(sortBy).ascending() : 
                    Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ChildUserResponseDTO> response = childUserService.getAllChildUsers(pageable, search);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all child users (list without pagination)
     * GET /v1/admin/children/all
     */
    @GetMapping("/all")
    @Operation(summary = "Get all child users (list format)")
    public ResponseEntity<List<ChildUserResponseDTO>> getAllChildUsersList() {
        log.info("REST request to get all child users as list");
        List<ChildUserResponseDTO> response = childUserService.getAllChildUsersList();
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get children by family ID
     * GET /v1/admin/children/family/{familyId}
     */
    @GetMapping("/family/{familyId}")
    @Operation(summary = "Get children by family ID")
    public ResponseEntity<List<ChildUserResponseDTO>> getChildrenByFamilyId(@PathVariable Long familyId) {
        log.info("REST request to get children for family ID: {}", familyId);
        List<ChildUserResponseDTO> response = childUserService.getChildrenByFamilyId(familyId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Delete child user (soft delete)
     * DELETE /v1/admin/children/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete child user")
    public ResponseEntity<Void> deleteChildUser(@PathVariable Long id) {
        log.info("REST request to delete child user with ID: {}", id);
        childUserService.deleteChildUser(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Enable/Disable child user
     * PATCH /v1/admin/children/{id}/status?enabled=true
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "Enable or disable child user")
    public ResponseEntity<ChildUserResponseDTO> setChildStatus(
            @PathVariable Long id,
            @RequestParam boolean enabled) {
        log.info("REST request to set status for child ID: {} to enabled: {}", id, enabled);
        ChildUserResponseDTO response = childUserService.setChildStatus(id, enabled);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get children count by family
     * GET /v1/admin/children/count/family/{familyId}
     */
    @GetMapping("/count/family/{familyId}")
    @Operation(summary = "Get children count by family ID")
    public ResponseEntity<Long> getChildrenCountByFamily(@PathVariable Long familyId) {
        log.info("REST request to get children count for family ID: {}", familyId);
        long count = childUserService.getChildrenCountByFamily(familyId);
        return ResponseEntity.ok(count);
    }

    /**
     * GET /v1/admin/children/download-excel
     *
     * Downloads a filtered Excel of all child users.
     * Each row includes full child details, complete family info,
     * parents count, and parent name(s) from the same family.
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
     *   createdFrom   – yyyy-MM-dd
     *   createdTo     – yyyy-MM-dd
     */
    @GetMapping("/download-excel")
    @Operation(summary = "Download filtered child users as Excel")
    public ResponseEntity<?> downloadChildUsersExcel(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String ageGroup,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) Boolean emailVerified,
            @RequestParam(required = false) String familyCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo) {
        log.info("REST request to download child users Excel");
        try {
            byte[] excel = childUserService.generateChildUsersExcel(
                    q, gender, enabled, ageGroup, minAge, maxAge,
                    emailVerified, familyCode, createdFrom, createdTo);

            String filename = "child-users-" + LocalDate.now() + ".xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excel);
        } catch (IOException e) {
            log.error("Child users Excel generation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate Excel: " + e.getMessage()));
        }
    }
}