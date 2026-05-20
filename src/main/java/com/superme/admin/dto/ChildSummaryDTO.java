package com.superme.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChildSummaryDTO {
    private Long id;
    private String name;
    private Integer age;
    private LocalDateTime lastLoginDate;
}
