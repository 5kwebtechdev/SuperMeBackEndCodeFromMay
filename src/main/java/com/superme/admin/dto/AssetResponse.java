package com.superme.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetResponse {
    private Long id;
    private String fileName;
    private String fileUrl;
    private String mimeType;
    private Long sizeBytes;
    private String formattedSize;
    private Long folderId;
    private String folderName;
    private Long uploadedById;
    private String uploadedBy;
    private String createdAt;
}