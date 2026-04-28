package com.superme.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "file_uploads")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Long adminId;
    private String fileName;        // S3 object key
    private String fileUrl;
    private String originalName;    // Name from UI
    private String contentType;
    private Long fileSize;        // in bytes

    @Lob
    private String presignedUrl;    // Optional: for debugging/logging

    private String status;          // PENDING, UPLOADED, FAILED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @ElementCollection
    @CollectionTable(name = "file_metadata", joinColumns = @JoinColumn(name = "file_id"))
    @MapKeyColumn(name = "meta_key")
    @Column(name = "meta_value")
    private Map<String, String> metadata;

    @Column(length = 100)
    private String entityType;


}

