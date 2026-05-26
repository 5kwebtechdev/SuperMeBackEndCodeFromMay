package com.superme.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class ArticleFileStorageService {

    @Value("${article.upload-dir}")
    private String articleUploadDir;

    @Value("${file.base-url}")
    private String fileBaseUrl;

    private String thumbnailsDir;
    private String attachmentsDir;
    private String contentDir;

    @PostConstruct
    public void init() {
        thumbnailsDir  = articleUploadDir + File.separator + "thumbnails"  + File.separator;
        attachmentsDir = articleUploadDir + File.separator + "attachments" + File.separator;
        contentDir     = articleUploadDir + File.separator + "content"     + File.separator;
        createDir(thumbnailsDir);
        createDir(attachmentsDir);
        createDir(contentDir);
    }

    private void createDir(String path) {
        File dir = new File(path);
        if (!dir.exists()) dir.mkdirs();
    }

    /** Saves thumbnail, returns filename only (UUID + ext). */
    public String saveThumbnail(MultipartFile file) {
        return saveFile(file, thumbnailsDir);
    }

    /** Saves attachment, returns filename only (UUID + ext). */
    public String saveAttachment(MultipartFile file) {
        return saveFile(file, attachmentsDir);
    }

    /** Saves content image, returns filename only (UUID + ext). */
    public String saveContentImage(MultipartFile file) {
        return saveFile(file, contentDir);
    }

    /** Full download URL for a thumbnail filename. */
    public String getThumbnailUrl(String filename) {
        if (filename == null || filename.isBlank()) return null;
        return fileBaseUrl + "/v1/admin/articles/download/thumbnail/" + filename;
    }

    /** Full download URL for an attachment filename. */
    public String getAttachmentUrl(String filename) {
        if (filename == null || filename.isBlank()) return null;
        return fileBaseUrl + "/v1/admin/articles/download/attachment/" + filename;
    }

    /** Full download URL for a content image filename. */
    public String getContentImageUrl(String filename) {
        if (filename == null || filename.isBlank()) return null;
        return fileBaseUrl + "/v1/admin/articles/download/content/" + filename;
    }

    public String getThumbnailsDir()  { return thumbnailsDir; }
    public String getAttachmentsDir() { return attachmentsDir; }
    public String getContentDir()     { return contentDir; }

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
            throw new RuntimeException("Failed to save article file: " + e.getMessage(), e);
        }
    }
}