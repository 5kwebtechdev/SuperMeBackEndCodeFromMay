package com.superme.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PresignRequestDto {
    private Long userId;
    private Long adminId;
    private String role;
    private String originalName;
    private String contentType;
    private Long fileSize;
    private Map<String, String> metadata;
}
