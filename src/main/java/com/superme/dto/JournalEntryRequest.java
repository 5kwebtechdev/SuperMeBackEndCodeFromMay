package com.superme.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class JournalEntryRequest {
    private String emotions;
    private String sleep;
    private String health;
    private String hobbies;
    private String food;
    private String social;
    private String school;
    private String notes;
    private List<MultipartFile> files; // ✅ instead of base64

    // Constructor for mapping
    public JournalEntryRequest() {}
}