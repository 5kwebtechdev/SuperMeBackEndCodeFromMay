package com.superme.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryIconUpdateRequest {

    @NotBlank
    private String category;

    @NotBlank
    private String oldValue;

    @NotBlank
    private String newValue;

    @NotBlank
    private String newIconPath;
}