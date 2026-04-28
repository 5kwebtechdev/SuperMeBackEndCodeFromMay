package com.superme.dto;

import lombok.Data;
import java.util.List;

@Data
public class AdminAdOverviewResponseDTO {
    private List<AdminAdDTO> ads;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;
    private AdStatistics statistics; // Changed to use the separate AdStatistics class
    private FilterOptions filterOptions;

    public AdminAdOverviewResponseDTO() {
        this.statistics = new AdStatistics();
        this.filterOptions = new FilterOptions();
    }

    @Data
    public static class FilterOptions {
        private List<String> adTypes;
        private List<String> targetScreens;
        private List<String> ageGroups;
        private List<String> genders;
        private List<String> priorities;
        private List<String> statuses;

        public FilterOptions() {}
    }

}