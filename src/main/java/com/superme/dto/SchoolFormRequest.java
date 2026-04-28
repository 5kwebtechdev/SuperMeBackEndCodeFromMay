package com.superme.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolFormRequest {

    private String logo;          // Logo URL from S3
    private String schoolName;
    private String schoolBranch;
    private String city;
    private String state;
    private List<String> className;
    private String genderType;
    private Integer catalog;      // can be null when creating; system may set it
    private String status;        // "Active" / "Inactive"
}