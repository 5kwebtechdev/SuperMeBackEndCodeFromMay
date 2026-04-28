package com.superme.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolListItem {

    private Long id;

    private String logo;          // LOGO
    private String schoolName;    // SCHOOL NAME
    private String schoolBranch;  // SCHOOL BRANCH
    private String city;          // CITY
    private String state;         // STATE
    private List<String> className;     // CLASS
    private String genderType;    // GENDER TYPE
    private Integer catalog;      // CATALOG
    private String status;        // STATUS
    // ACTIONS are UI buttons using the id
}