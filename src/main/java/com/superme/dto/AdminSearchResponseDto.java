package com.superme.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminSearchResponseDto {

    private List<UserResponseAdminDto> users;
    private UserStatisticsDto statistics;
    // ✅ Pagination metadata
    private int page;          // current page number (0-based or 1-based as you prefer)
    private int size;          // size per page
    private long totalElements; // total records in DB for this filter
    private int totalPages;
}
