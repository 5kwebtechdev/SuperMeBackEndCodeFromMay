// CategoryRequestDTO.java
package com.superme.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class CategoryRequestDTO {

    @NotBlank(message = "Category name is required")
    private String categoryName;

    private String description;

    private String status;

    private String thumbnail;
}