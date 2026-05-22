package com.superme.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class FileStorageService2 {

    @Value("${tutor.upload-dir}")
    private String tutorUploadDir;

    @Value("${file.upload-dir}")
    private String fileUploadDir;

    @Value("${file.base-url}")
    private String fileBaseUrl;

    private String profilePicsDir;
    private String documentsDir;
    private String contentImagesDir;
    private String thumbnailsDir;
    private String attachmentsDir;

    @PostConstruct
    public void init() {
        profilePicsDir  = tutorUploadDir + File.separator + "profile-pictures" + File.separator;
        documentsDir    = tutorUploadDir + File.separator + "documents"         + File.separator;
        contentImagesDir = fileUploadDir + File.separator + "articles" + File.separator + "content"     + File.separator;
        thumbnailsDir    = fileUploadDir + File.separator + "articles" + File.separator + "thumbnails"  + File.separator;
        attachmentsDir   = fileUploadDir + File.separator + "articles" + File.separator + "attachments" + File.separator;

        createDirectoryIfNotExists(profilePicsDir);
        createDirectoryIfNotExists(documentsDir);
        createDirectoryIfNotExists(contentImagesDir);
        createDirectoryIfNotExists(thumbnailsDir);
        createDirectoryIfNotExists(attachmentsDir);
    }

    private void createDirectoryIfNotExists(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    // Returns relative sub-path like "profile-pictures/profile_1_20260521_123456.jpg"
    public String saveProfilePicture(Long tutorId, MultipartFile file) {
        return saveTutorFile(file, profilePicsDir, "profile_" + tutorId, "profile-pictures");
    }

    // Returns relative sub-path like "documents/doc_1_20260521_123456.pdf"
    public String saveDocument(Long tutorId, MultipartFile file) {
        return saveTutorFile(file, documentsDir, "doc_" + tutorId, "documents");
    }

    public String saveArticleThumbnail(Long articleId, MultipartFile file) {
        return saveArticleFile(file, thumbnailsDir, "thumbnail_" + articleId);
    }

    public String saveArticleContentImage(Long articleId, int index, MultipartFile file) {
        return saveArticleFile(file, contentImagesDir, "content_" + articleId + "_" + index);
    }

    public String saveArticleAttachment(Long articleId, MultipartFile file) {
        return saveArticleFile(file, attachmentsDir, "attachment_" + articleId);
    }

    // Saves a tutor file and returns a sub-path relative to tutorUploadDir, e.g. "profile-pictures/file.jpg"
    private String saveTutorFile(MultipartFile file, String directory, String baseName, String subDir) {
        if (file == null || file.isEmpty()) return null;
        try {
            String fileName = baseName + "_" + timestamp() + ext(file);
            Files.copy(file.getInputStream(), Paths.get(directory + fileName));
            return subDir + "/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save file: " + e.getMessage(), e);
        }
    }

    // Saves an article file and returns a path relative to fileUploadDir
    private String saveArticleFile(MultipartFile file, String directory, String baseName) {
        if (file == null || file.isEmpty()) return null;
        try {
            String fileName = baseName + "_" + timestamp() + ext(file);
            String fullPath = directory + fileName;
            Files.copy(file.getInputStream(), Paths.get(fullPath));
            return fullPath.replace(fileUploadDir, "").replace("\\", "/");
        } catch (IOException e) {
            throw new RuntimeException("Failed to save file: " + e.getMessage(), e);
        }
    }

    public boolean deleteFile(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) return false;
        try {
            // Try tutor dir first, then general upload dir
            File f = new File(tutorUploadDir + File.separator + relativePath.replace("/", File.separator));
            if (f.exists()) return f.delete();
            f = new File(fileUploadDir + File.separator + relativePath.replace("/", File.separator));
            return f.delete();
        } catch (Exception e) {
            return false;
        }
    }

    // Returns the full download URL for a tutor file sub-path
    // e.g. "profile-pictures/file.jpg" → "https://api.supermeapp.com/v1/admin/tutors/download/profile-pictures/file.jpg"
    public String getFullUrl(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) return null;
        return fileBaseUrl + "/v1/admin/tutors/download/" + relativePath;
    }

    private String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }

    private String ext(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name != null && name.contains(".")) return name.substring(name.lastIndexOf("."));
        return "";
    }
}