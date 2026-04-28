package com.superme.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonBackReference;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "challenge_attachment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChallengeAttachment {

    public enum FileType {
        JPG("image/jpeg"),
        PNG("image/png"),
        PDF("application/pdf");

        private final String mimeType;

        FileType(String mimeType) {
            this.mimeType = mimeType;
        }

        public String getMimeType() {
            return mimeType;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "challenge_id")
    @JsonBackReference("challenge-attachments")
    private Challenge challenge;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false)
    private String fileUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FileType fileType;

    @Column(nullable = false)
    private Long fileSize;

    // Audit fields
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now(ZoneId.systemDefault());
    }

    public boolean isImage() {
        return fileType == FileType.JPG || fileType == FileType.PNG;
    }

    public boolean isPdf() {
        return fileType == FileType.PDF;
    }

    @Override
    public String toString() {
        return "ChallengeAttachment{" +
                "id=" + id +
                ", fileName='" + fileName + '\'' +
                ", fileType=" + fileType +
                '}';
    }
}