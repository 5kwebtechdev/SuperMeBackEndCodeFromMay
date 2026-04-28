package com.superme.dto;

import lombok.Data;

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
    private List<String> attachmentUrls;

    // Constructor for mapping
    public JournalEntryRequest() {}
}