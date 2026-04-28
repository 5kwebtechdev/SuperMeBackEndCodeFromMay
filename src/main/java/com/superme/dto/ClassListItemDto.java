package com.superme.dto;

import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ClassListItemDto { // classId
    private List<String> name;          // "Class 7"
}

