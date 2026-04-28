package com.superme.dto;

import com.superme.enums.Relationship;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminCreateUserDTO {
  // Removed username field
  private String name;
  private String gender;
  private String email;
  private String phone;
  private String avatarName;
  private String petName;
  private LocalDate dateOfBirth;

  // Added relationship field as enum
  private Relationship relationship;

  }
