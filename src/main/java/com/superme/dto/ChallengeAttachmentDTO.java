package com.superme.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChallengeAttachmentDTO {
    private Long id;
    private String fileName;
    private String fileUrl;
    private String fileType; // JPG, PNG, PDF
    private Long fileSize; // in bytes

    // Helper method to get file size in MB
    public double getFileSizeInMB() {
        return fileSize != null ? (double) fileSize / (1024 * 1024) : 0.0;
    }
}