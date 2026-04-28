// CategoryResponseDTO.java
package com.superme.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CategoryResponseDTO {
    private Long id;
    private String categoryName;
    private String description;
    private String thumbnailUrl;
    private String status;
    private int courseCount;
}