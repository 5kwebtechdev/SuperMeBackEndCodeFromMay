package com.superme.dto;

import com.superme.model.ChallengeAttachment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeAttachmentResponseDTO {

    private Long attachmentId;
    private String fileName;
    private String fileUrl;
    private ChallengeAttachment.FileType fileType;
    private Long fileSize;

    // Audit fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean isImage() {
        return fileType == ChallengeAttachment.FileType.JPG ||
                fileType == ChallengeAttachment.FileType.PNG;
    }

    public String getFileSizeFormatted() {
        if (fileSize == null) return "N/A";
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
    }
}