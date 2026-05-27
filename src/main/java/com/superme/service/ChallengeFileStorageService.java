package com.superme.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class ChallengeFileStorageService {

    @Value("${challenge.upload-dir}")
    private String challengeUploadDir;

    @Value("${file.base-url}")
    private String fileBaseUrl;

    private String thumbnailsDir;
    private String attachmentsDir;
    private String questionVisualsDir;

    @PostConstruct
    public void init() {
        thumbnailsDir      = challengeUploadDir + File.separator + "thumbnails"      + File.separator;
        attachmentsDir     = challengeUploadDir + File.separator + "attachments"     + File.separator;
        questionVisualsDir = challengeUploadDir + File.separator + "questionVisuals" + File.separator;
        createDir(thumbnailsDir);
        createDir(attachmentsDir);
        createDir(questionVisualsDir);
    }

    private void createDir(String path) {
        File dir = new File(path);
        if (!dir.exists()) dir.mkdirs();
    }

    /** Saves thumbnail and returns the filename only (UUID + ext). */
    public String saveThumbnail(MultipartFile file) {
        return saveFile(file, thumbnailsDir);
    }

    /** Saves attachment and returns the filename only (UUID + ext). */
    public String saveAttachment(MultipartFile file) {
        return saveFile(file, attachmentsDir);
    }

    /** Deletes a thumbnail by filename. */
    public boolean deleteThumbnail(String filename) {
        return deleteFile(thumbnailsDir + filename);
    }

    /** Deletes an attachment by filename. */
    public boolean deleteAttachment(String filename) {
        return deleteFile(attachmentsDir + filename);
    }

    /** Saves a question visual and returns the filename only (UUID + ext). */
    public String saveQuestionVisual(MultipartFile file) {
        return saveFile(file, questionVisualsDir);
    }

    /** Deletes a question visual by filename. */
    public boolean deleteQuestionVisual(String filename) {
        return deleteFile(questionVisualsDir + filename);
    }

    /** Full download URL for a question visual filename. */
    public String getQuestionVisualUrl(String filename) {
        if (filename == null || filename.isBlank()) return null;
        return fileBaseUrl + "/v1/admin/challenges/download/question-visual/" + filename;
    }

    /** Returns the absolute directory path for question visuals (used by download endpoint). */
    public String getQuestionVisualsDir() { return questionVisualsDir; }

    /** Full download URL for a thumbnail filename. */
    public String getThumbnailUrl(String filename) {
        if (filename == null || filename.isBlank()) return null;
        return fileBaseUrl + "/v1/admin/challenges/download/thumbnail/" + filename;
    }

    /** Full download URL for an attachment filename. */
    public String getAttachmentUrl(String filename) {
        if (filename == null || filename.isBlank()) return null;
        return fileBaseUrl + "/v1/admin/challenges/download/attachment/" + filename;
    }

    /** Returns the absolute directory path for thumbnails (used by download endpoint). */
    public String getThumbnailsDir() { return thumbnailsDir; }

    /** Returns the absolute directory path for attachments (used by download endpoint). */
    public String getAttachmentsDir() { return attachmentsDir; }

    private String saveFile(MultipartFile file, String directory) {
        if (file == null || file.isEmpty()) return null;
        try {
            String ext = "";
            String original = file.getOriginalFilename();
            if (original != null && original.contains("."))
                ext = original.substring(original.lastIndexOf("."));
            String filename = UUID.randomUUID() + ext;
            Files.copy(file.getInputStream(), Paths.get(directory + filename));
            return filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save challenge file: " + e.getMessage(), e);
        }
    }

    private boolean deleteFile(String fullPath) {
        try {
            File f = new File(fullPath);
            return f.exists() && f.delete();
        } catch (Exception e) {
            return false;
        }
    }
}