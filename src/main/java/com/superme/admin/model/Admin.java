package com.superme.admin.model;

import com.superme.enums.Department;
import com.superme.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class Admin {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String fullName;
  private String email;
  private String phone;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Department department;

  private String designation;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role = Role.ADMIN;

  private LocalDateTime expiryDate;

  @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
  @JoinColumn(name = "password_id")
  private AdminPassword password;


}
