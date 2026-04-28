package com.superme.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PresignResponseDto {
    private Long fileId;
    private String fileName;
    private String fileUrl;
    private String presignedUrl;
    private String status;
    private Map<String, String> requiredHeaders;
}
