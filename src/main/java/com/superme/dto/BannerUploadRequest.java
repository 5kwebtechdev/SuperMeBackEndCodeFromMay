package com.superme.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class BannerUploadRequest {
    private MultipartFile bannerImage;
    private Long userId;
    private String adName;
}