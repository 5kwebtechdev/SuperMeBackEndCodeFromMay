package com.superme.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "schools")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class School {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LOGO (S3 URL)
    private String logo;

    // SCHOOL NAME
    private String schoolName;

    // SCHOOL BRANCH
    private String schoolBranch;

    // CITY
    private String city;

    // STATE
    private String state;

    @OneToMany(mappedBy = "school", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SchoolClass> classes = new ArrayList<>();

    // GENDER TYPE (Co-ed / Boys / Girls)
    private String genderType;

    // CATALOG (number of products in this school’s catalog)
    private Integer catalog;

    // STATUS (Active / Inactive)
    private String status;
}
