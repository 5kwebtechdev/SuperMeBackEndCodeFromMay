package com.superme.admin.dto;

import com.superme.enums.Department;
import lombok.Data;

@Data
public class UpdateAdminRequest {
  private Long adminId;
  private String fullName;
  private String phone;
  private Department department;
  private String designation;


}
