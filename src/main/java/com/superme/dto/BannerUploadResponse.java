package com.superme.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class BannerUploadResponse {
    private String s3Key;
    private String presignedUrl;
    private String publicUrl;
    private String status;
    private String message;
}