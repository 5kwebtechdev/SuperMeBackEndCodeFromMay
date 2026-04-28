package com.superme.dto;


import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SchoolListItemDto {
    private Long id;              // schoolId
    private String logoUrl;
    private String name;          // "St. Xavier's High School"
    private String branch;        // "Gandhi Nagar"
    private String city;
    private String state;
}
