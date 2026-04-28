package com.superme.dto;

import com.superme.model.ChallengeAttachment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating/updating challenge attachments
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeAttachmentRequestDTO {

    @NotBlank(message = "File name is required")
    private String fileName;

    @NotBlank(message = "File URL is required")
    private String fileUrl;

    @NotNull(message = "File type is required")
    private ChallengeAttachment.FileType fileType;

    @NotNull(message = "File size is required")
    @Positive(message = "File size must be positive")
    private Long fileSize;

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