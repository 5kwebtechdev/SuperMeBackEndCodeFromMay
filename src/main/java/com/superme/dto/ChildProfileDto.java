package com.superme.dto;


import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ChildProfileDto {

    private Long id;              // childId
    private String childName;
    private Long schoolId;
    private String schoolName;
    private Long classId;
    private String className;
    private Boolean isDefault;
}
