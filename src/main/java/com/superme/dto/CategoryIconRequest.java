package com.superme.dto;

import lombok.Data;

@Data
public class CategoryIconRequest {
    private String category;
    private String value;
    private String iconPath;
}