package com.superme.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JournalEntryResponseDTO {
    private Long id;
    private Long createdBy; // Only user ID
    private LocalDate creationDate;
    private LocalTime creationTime;
    private String emotions;
    private String sleep;
    private String health;
    private String hobbies;
    private String food;
    private String social;
    private String school;
    private String notes;
//    private List<String> attachmentUrls;
    private List<String> attachments; // URLs for frontend
}