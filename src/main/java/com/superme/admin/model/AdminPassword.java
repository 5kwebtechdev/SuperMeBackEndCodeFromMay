package com.superme.admin.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity representing admin password, stored separately.
 */
@Entity
@Getter
@Setter
public class AdminPassword {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String hashedPassword;
  private LocalDateTime createdAt;
  private Boolean active;
}
