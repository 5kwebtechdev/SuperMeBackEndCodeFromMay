package com.superme.admin.dto;

public class FilterCriteria {
    private String searchTerm;
    private String type;
    private String difficulty;
    private String ageGroup;
    private String status;
    
    // Getters and setters
    public String getSearchTerm() { return searchTerm; }
    public void setSearchTerm(String searchTerm) { this.searchTerm = searchTerm; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    
    public String getAgeGroup() { return ageGroup; }
    public void setAgeGroup(String ageGroup) { this.ageGroup = ageGroup; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}