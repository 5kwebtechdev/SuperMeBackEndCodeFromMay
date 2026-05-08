package com.superme.admin.controller;

import com.superme.admin.dto.*;
import com.superme.admin.service.AuthService;
import com.superme.exception.BusinessException;
import com.superme.exception.InternalServerErrorException;
import com.superme.exception.ResourceNotFoundException;
import com.superme.exception.UnauthorizedActionException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/admin/auth")
public class AuthController {

  @Autowired
  private AuthService authService;


  @PostConstruct
  public void init() {
    System.out.println("AuthController Loaded ✅");
  }







  @PostMapping("/register")
  public boolean registerAdmin(
    @RequestHeader(value = "Authorization", required = false) String token,
    @RequestBody RegisterAdminRequest request
  ) throws Exception {
    return authService.registerAdmin(
      token != null ? token.replace("Bearer ", "") : null,
     request
    );
  }

  @DeleteMapping("/delete-user/{userId}")
  public ResponseEntity<String> deleteAccount(@PathVariable Long userId, Principal principal) {
    try {
      String message = authService.deleteAccount(userId, principal.getName());
      return ResponseEntity.ok(message);
    } catch (UnauthorizedActionException e) {
      throw new UnauthorizedActionException(e.getMessage());
    } catch (ResourceNotFoundException e) {
      throw new ResourceNotFoundException("User not found.");
    } catch (Exception e) {
      throw new InternalServerErrorException("An error occurred while deleting the account.");
    }
  }



  @PostMapping("/login")
  public ResponseEntity<?> adminLogin(@RequestBody LoginRequest request) {
        System.out.println("1.1");
      try {
           AdminLoginResponse response = authService.login(request.getEmail(), request.getPassword());
          return ResponseEntity.ok(response);

      } catch (BusinessException ex) {
           throw ex;
      } catch (Exception ex) {
          throw new BusinessException("An unexpected error occurred. Please try again later.");
      }
  }


  /**
   * Updates an admin's details. Only ADMIN can update.
   */
  @PutMapping("/update")
  public ResponseEntity<?> updateAdmin(
          @RequestHeader("Authorization") String token,
          @RequestBody UpdateAdminRequest request
  ) {
    try {
      boolean updated = authService.updateAdmin(
              token.replace("Bearer ", ""),
              request.getAdminId(),
              request.getFullName(),
              request.getPhone(),
              request.getDepartment(),
              request.getDesignation()
      );

      if (!updated) {
        throw new BusinessException("Failed to update admin. Please check permissions or admin ID.");
      }

      return ResponseEntity.ok(Map.of(
              "message", "Admin updated successfully",
              "status", 200
      ));

    } catch (BusinessException ex) {
      // Let GlobalExceptionHandler handle this
      throw ex;

    } catch (Exception ex) {
      // Wrap unexpected exceptions
      throw new BusinessException("An unexpected error occurred while updating admin.");
    }
  }


  /**
   * Deletes an admin by ID. Only ADMIN can delete.
   */
  @DeleteMapping("/delete")
  public boolean deleteAdmin(
    @RequestHeader("Authorization") String token,
    @RequestBody DeleteAdminRequest request
  ) {
    return authService.deleteAdmin(
      token.replace("Bearer ", ""),
      request.getAdminId()
    );
  }
}
