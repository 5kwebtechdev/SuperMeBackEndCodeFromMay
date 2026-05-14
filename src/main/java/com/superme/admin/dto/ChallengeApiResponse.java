package com.superme.admin.dto;

import java.util.List;

public class ChallengeApiResponse {
    private List<ChallengeResponseDTO> challenges;
    private ChallengeStatistics statistics;
    private FilterCriteria filterCriteria;
    private int totalCount;
    private int filteredCount;
    private boolean hasMore;
    private String message;
    private boolean success;
    private boolean filtered;
    private int filteredOutCount;
    
    // Getters and setters
    public List<ChallengeResponseDTO> getChallenges() { return challenges; }
    public void setChallenges(List<ChallengeResponseDTO> challenges) { this.challenges = challenges; }
    
    public ChallengeStatistics getStatistics() { return statistics; }
    public void setStatistics(ChallengeStatistics statistics) { this.statistics = statistics; }
    
    public FilterCriteria getFilterCriteria() { return filterCriteria; }
    public void setFilterCriteria(FilterCriteria filterCriteria) { this.filterCriteria = filterCriteria; }
    
    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    
    public int getFilteredCount() { return filteredCount; }
    public void setFilteredCount(int filteredCount) { this.filteredCount = filteredCount; }
    
    public boolean isHasMore() { return hasMore; }
    public void setHasMore(boolean hasMore) { this.hasMore = hasMore; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public boolean isFiltered() { return filtered; }
    public void setFiltered(boolean filtered) { this.filtered = filtered; }
    
    public int getFilteredOutCount() { return filteredOutCount; }
    public void setFilteredOutCount(int filteredOutCount) { this.filteredOutCount = filteredOutCount; }
}
