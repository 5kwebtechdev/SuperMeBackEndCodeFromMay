package com.superme.admin.controller;


import com.superme.admin.dto.ParentUserRequestDTO;
import com.superme.admin.dto.ParentUserResponseDTO;
import com.superme.admin.service.ParentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

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
}