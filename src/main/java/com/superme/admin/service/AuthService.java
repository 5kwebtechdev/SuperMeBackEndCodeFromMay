package com.superme.admin.service;

import com.superme.admin.dto.AdminLoginResponse;
import com.superme.admin.dto.RegisterAdminRequest;
import com.superme.admin.model.Admin;
import com.superme.admin.model.AdminPassword;
import com.superme.admin.repository.AdminRepository;
import com.superme.admin.repository.AdminPasswordRepository;
import com.superme.admin.security.AdminJwtUtil;
import com.superme.enums.Department;
import com.superme.enums.Role;
import com.superme.exception.BusinessException;
import com.superme.exception.ResourceNotFoundException;
import com.superme.exception.UnauthorizedActionException;
import com.superme.model.User;
import com.superme.repository.UserPasswordRepository;
import com.superme.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for handling admin authentication and management logic.
 */
@Service
public class AuthService {
  @Autowired
  private AdminRepository adminRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UserPasswordRepository userPasswordRepository;
  @Autowired
  private AdminPasswordRepository adminPasswordRepository;
  @Autowired
  private AdminJwtUtil jwtUtil;

  private final PasswordEncoder passwordEncoder;

  @Autowired
  public AuthService(PasswordEncoder passwordEncoder) {
    this.passwordEncoder = passwordEncoder;
  }



    public String deleteAccount(Long userId, String principalName) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new ResourceNotFoundException("User not found");
        }
        User userToDelete = userOptional.get();
        // Only allow if principalName matches user email/phone/id (as string)
//        if (!principalName.equals(String.valueOf(userToDelete.getId())) &&
//                (userToDelete.getEmail() == null || !principalName.equals(userToDelete.getEmail())) &&
//                (userToDelete.getPhone() == null || !principalName.equals(userToDelete.getPhone()))) {
//            throw new UnauthorizedActionException("You are not authorized to delete this account.");
//        }
        userPasswordRepository.findByUser(userToDelete)
                .ifPresent(userPasswordRepository::delete);
        userRepository.delete(userToDelete);
        return "Account deleted successfully!";
    }





    /**
   * Registers a new admin if the requester is an ADMIN.
   */
  public boolean registerAdmin(String requesterToken, RegisterAdminRequest dto) {
    try {
      boolean bootstrapRegistration = adminRepository.count() == 0;
      if (!bootstrapRegistration) {
        if (requesterToken == null || requesterToken.isBlank()) {
          throw new BusinessException("Authorization token is required");
        }

        String requesterRole = jwtUtil.extractRole(requesterToken);
        if (!"SUPER_ADMIN".equals(requesterRole)) {
          throw new BusinessException("Only Super admin can register new admins");
        }
      }

      if (adminRepository.findByEmail(dto.getEmail()).isPresent()) {
        throw new BusinessException("Email already in use");
      }

      if (adminRepository.findByPhone(dto.getPhone()).isPresent()) {
        throw new BusinessException("Phone number already in use");
      }

      if (dto.getPassword() == null || dto.getPassword().length() < 8) {
        throw new BusinessException("Password must be at least 8 characters long");
      }

      Admin admin = new Admin();
      admin.setFullName(dto.getFullName());
      admin.setEmail(dto.getEmail());
      admin.setPhone(dto.getPhone());
      admin.setDepartment(dto.getDepartment());
      admin.setDesignation(dto.getDesignation());
      admin.setRole(dto.getRole() != null ? dto.getRole() : Role.ADMIN);
      admin.setExpiryDate(dto.getExpiryDate() != null
              ? dto.getExpiryDate()
              : LocalDateTime.now().plusYears(1));

      AdminPassword password = new AdminPassword();
      password.setHashedPassword(passwordEncoder.encode(dto.getPassword()));
      password.setCreatedAt(LocalDateTime.now());
      password.setActive(true);

      admin.setPassword(password);
      adminRepository.save(admin);

      return true;

    } catch (BusinessException ex) {
      throw ex; // rethrow custom exceptions

    } catch (Exception ex) {
      throw new BusinessException("An unexpected error occurred while registering admin.");
    }
  }



  /**
   * Authenticates an admin and returns a JWT token if successful.
   */




  public AdminLoginResponse login(String email, String password) {
        try {
            // 1️⃣ Validate inputs
            if (email == null || email.isBlank() || password == null || password.isBlank()) {
                throw new BusinessException("Email and password are required");
            }

            // 2️⃣ Find admin by email
            Admin admin = adminRepository.findByEmail(email)
                    .orElseThrow(() -> new BusinessException("Invalid email or password"));

            // 3️⃣ Check password exists
            if (admin.getPassword() == null || admin.getPassword().getHashedPassword() == null) {
                throw new BusinessException("Password not set for this admin");
            }

            String hashedPassword = admin.getPassword().getHashedPassword();

            // 4️⃣ Compare raw password with hashed password
            if (!passwordEncoder.matches(password, hashedPassword)) {
                throw new BusinessException("Invalid email or password");
            }

            // 5️⃣ Check account expiry
            boolean isActive = true;
            if (admin.getExpiryDate() != null && admin.getExpiryDate().isBefore(LocalDateTime.now())) {
                isActive = false;
                throw new BusinessException("Account expired");
            }

            // 6️⃣ Generate JWT token
            String token = jwtUtil.generateToken(
                    admin.getId(),
                    admin.getFullName(),
                    admin.getEmail(),
                    admin.getRole().name(),
                    admin.getDepartment() != null ? admin.getDepartment().name() : null,
                    admin.getDesignation()
            );

            // 7️⃣ Create admin details
            AdminLoginResponse.AdminDetails adminDetails = new AdminLoginResponse.AdminDetails();
            adminDetails.setId(admin.getId());
            adminDetails.setFullName(admin.getFullName());
            adminDetails.setEmail(admin.getEmail());
            adminDetails.setPhone(admin.getPhone());
            adminDetails.setDepartment(admin.getDepartment() != null ? admin.getDepartment().name() : null);
            adminDetails.setDesignation(admin.getDesignation());
            adminDetails.setRole(admin.getRole().name());
            adminDetails.setExpiryDate(admin.getExpiryDate());
            adminDetails.setActive(isActive);

            // 8️⃣ Return the complete response
            return new AdminLoginResponse(200, token, adminDetails);

        } catch (BusinessException ex) {
            throw ex; // handled by GlobalExceptionHandler
        } catch (Exception ex) {
            throw new BusinessException("An unexpected error occurred during login. Please try again later.");
        }
    }



  /**
   * Updates an existing admin's details. Only ADMIN can update.
   * @param requesterToken JWT token of the requester
   * @param adminId ID of the admin to update
   * @param fullName New full name (nullable)
   * @param phone New phone (nullable)
   * @param department New department (nullable)
   * @param designation New designation (nullable)
   * @return true if update is successful, false otherwise
   */
  public boolean updateAdmin(String requesterToken, Long adminId, String fullName, String phone, Department department, String designation) {
    try {
      // 1️⃣ Check requester role
      String requesterRole = jwtUtil.extractRole(requesterToken);
      if (requesterRole == null || !requesterRole.equals("ADMIN")) {
        throw new BusinessException("Only ADMIN users can update admin details");
      }

      // 2️⃣ Find admin by ID
      Admin admin = adminRepository.findById(adminId)
              .orElseThrow(() -> new BusinessException("Admin not found with id: " + adminId));

      // 3️⃣ Validate phone uniqueness if updating
      if (phone != null && !phone.equals(admin.getPhone())) {
        if (adminRepository.findByPhone(phone).isPresent()) {
          throw new BusinessException("Phone number already in use");
        }
        admin.setPhone(phone);
      }

      // 4️⃣ Update other fields if provided
      if (fullName != null) admin.setFullName(fullName);
      if (department != null) admin.setDepartment(department);
      if (designation != null) admin.setDesignation(designation);

      // 5️⃣ Save changes
      adminRepository.save(admin);

      return true;

    } catch (BusinessException ex) {
      throw ex; // handled by GlobalExceptionHandler

    } catch (Exception ex) {
      throw new BusinessException("An unexpected error occurred while updating admin");
    }
  }

  public boolean existsByEmail(String email) {
      if (email == null || email.isBlank()) return false;
      return adminRepository.findByEmail(email.trim()).isPresent();
  }

  public boolean existsByPhone(String phone) {
      if (phone == null || phone.isBlank()) return false;
      return adminRepository.findByPhone(phone.trim()).isPresent();
  }

  /**
   * Optional convenience login for unified router (when the client uses phone).
   * Admins can still use /admin/auth/login with email/password directly.
   */
  public AdminLoginResponse loginByPhone(String phone, String password) {
      try {
          if (phone == null || phone.isBlank() || password == null || password.isBlank()) {
              throw new BusinessException("Phone and password are required");
          }

          Admin admin = adminRepository.findByPhone(phone.trim())
                  .orElseThrow(() -> new BusinessException("Invalid phone or password"));

          if (admin.getPassword() == null || admin.getPassword().getHashedPassword() == null) {
              throw new BusinessException("Password not set for this admin");
          }

          if (!passwordEncoder.matches(password, admin.getPassword().getHashedPassword())) {
              throw new BusinessException("Invalid phone or password");
          }

          if (admin.getExpiryDate() != null && admin.getExpiryDate().isBefore(LocalDateTime.now())) {
              throw new BusinessException("Account expired");
          }

          String token = jwtUtil.generateToken(
                  admin.getId(),
                  admin.getFullName(),
                  admin.getEmail(),
                  admin.getRole().name(),
                  admin.getDepartment() != null ? admin.getDepartment().name() : null,
                  admin.getDesignation()
          );

          AdminLoginResponse.AdminDetails adminDetails = new AdminLoginResponse.AdminDetails();
          adminDetails.setId(admin.getId());
          adminDetails.setFullName(admin.getFullName());
          adminDetails.setEmail(admin.getEmail());
          adminDetails.setPhone(admin.getPhone());
          adminDetails.setDepartment(admin.getDepartment() != null ? admin.getDepartment().name() : null);
          adminDetails.setDesignation(admin.getDesignation());
          adminDetails.setRole(admin.getRole().name());
          adminDetails.setExpiryDate(admin.getExpiryDate());
          adminDetails.setActive(true);

          return new AdminLoginResponse(200, token, adminDetails);

      } catch (BusinessException ex) {
          throw ex;
      } catch (Exception ex) {
          throw new BusinessException("An unexpected error occurred during login. Please try again later.");
      }
  }


  /**
   * Deletes an admin by ID. Only ADMIN can delete.
   * @param requesterToken JWT token of the requester
   * @param adminId ID of the admin to delete
   * @return true if deletion is successful, false otherwise
   */
//  public boolean deleteAdmin(String requesterToken, Long adminId) {
//    String requesterRole = jwtUtil.extractRole(requesterToken);
//    if (requesterRole == null || !requesterRole.equals("ADMIN")) {
//      return false;
//    }
//    Admin admin = adminRepository.findById(adminId).orElse(null);
//    if (admin == null) {
//      return false;
//    }
//    adminPasswordRepository.deleteById(adminId);
//    adminRepository.deleteById(adminId);
//    return true;
//  }
  public boolean deleteAdmin(String requesterToken, Long adminId) {

      String requesterRole = jwtUtil.extractRole(requesterToken);
      if (requesterRole == null || !requesterRole.equals("ADMIN")) {
          return false;
      }

      Admin admin = adminRepository.findById(adminId).orElse(null);
      if (admin == null) {
          return false;
      }

      // ✅ Only delete admin
      adminRepository.delete(admin);

      return true;
  }
    public Admin getAdminById(Long id) {
        return adminRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found with id: " + id));
    }


    public List<Admin> getAllUsersByRole(Role role) {
        return adminRepository.findByRole(role);
    }




}
