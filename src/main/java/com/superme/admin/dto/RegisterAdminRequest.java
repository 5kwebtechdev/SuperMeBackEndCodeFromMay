package com.superme.admin.dto;

import com.superme.enums.Department;
import com.superme.enums.Role;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class RegisterAdminRequest {
  private String fullName;
  private String gender;
  private String email;
  private String phone;
  private String petName;
  private String avatarName;
  private LocalDate dateOfBirth;
  private Department department;
  private String designation;
  private Role role;
  private String password;
  private LocalDateTime expiryDate;
}
