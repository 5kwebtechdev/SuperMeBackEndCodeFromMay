package com.superme.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoteResponse {
    private Long id;
    private String title;
    private String content;
    private List<String> tags;
    private java.time.LocalDate createdDate;
    private java.time.LocalTime createdTime;
    private java.time.LocalDate updatedDate;
    private java.time.LocalTime updatedTime;
    private Long userId;
}