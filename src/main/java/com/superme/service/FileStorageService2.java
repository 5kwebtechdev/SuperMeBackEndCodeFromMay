package com.superme.service;

//public class FileStorageService2 {
//}


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

    private static final String PROJECT_ROOT = System.getProperty("user.dir");
    private static final String UPLOADS_DIR = PROJECT_ROOT + File.separator + "uploads" + File.separator;

    // Subdirectories
    private static final String PROFILE_PICS_DIR = UPLOADS_DIR + "tutors" + File.separator + "profile-pictures" + File.separator;
    private static final String DOCUMENTS_DIR = UPLOADS_DIR + "tutors" + File.separator + "documents" + File.separator;
    private static final String CONTENT_IMAGES_DIR = UPLOADS_DIR + "articles" + File.separator + "content" + File.separator;
    private static final String THUMBNAILS_DIR = UPLOADS_DIR + "articles" + File.separator + "thumbnails" + File.separator;
    private static final String ATTACHMENTS_DIR = UPLOADS_DIR + "articles" + File.separator + "attachments" + File.separator;

    // Static initializer to create directories
    static {
        createDirectories();
    }

    private static void createDirectories() {
        createDirectoryIfNotExists(PROFILE_PICS_DIR);
        createDirectoryIfNotExists(DOCUMENTS_DIR);
        createDirectoryIfNotExists(CONTENT_IMAGES_DIR);
        createDirectoryIfNotExists(THUMBNAILS_DIR);
        createDirectoryIfNotExists(ATTACHMENTS_DIR);
    }

    private static void createDirectoryIfNotExists(String dirPath) {
        File directory = new File(dirPath);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (created) {
                System.out.println("Created directory: " + dirPath);
            } else {
                System.err.println("Failed to create directory: " + dirPath);
            }
        }
    }

    /**
     * Save profile picture for tutor
     */
    public String saveProfilePicture(Long tutorId, MultipartFile file) {
        return saveFile(file, PROFILE_PICS_DIR, "profile_" + tutorId);
    }

    /**
     * Save verification document for tutor
     */
    public String saveDocument(Long tutorId, MultipartFile file) {
        return saveFile(file, DOCUMENTS_DIR, "doc_" + tutorId);
    }

    /**
     * Save article thumbnail
     */
    public String saveArticleThumbnail(Long articleId, MultipartFile file) {
        return saveFile(file, THUMBNAILS_DIR, "thumbnail_" + articleId);
    }

    /**
     * Save article content image
     */
    public String saveArticleContentImage(Long articleId, int index, MultipartFile file) {
        return saveFile(file, CONTENT_IMAGES_DIR, "content_" + articleId + "_" + index);
    }

    /**
     * Save article attachment
     */
    public String saveArticleAttachment(Long articleId, MultipartFile file) {
        return saveFile(file, ATTACHMENTS_DIR, "attachment_" + articleId);
    }

    /**
     * Generic file save method
     */
    private String saveFile(MultipartFile file, String directory, String baseName) {
        try {
            if (file == null || file.isEmpty()) {
                return null;
            }

            // Get file extension
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // Create unique filename with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = baseName + "_" + timestamp + extension;

            // Full path
            String filePath = directory + fileName;

            // Save file
            Path path = Paths.get(filePath);
            Files.copy(file.getInputStream(), path);

            // Return relative URL for frontend
            // Get relative path from project root
            String relativePath = filePath.replace(PROJECT_ROOT, "").replace("\\", "/");
            return relativePath;

        } catch (IOException e) {
            throw new RuntimeException("Failed to save file: " + e.getMessage(), e);
        }
    }

    /**
     * Delete file by path
     */
    public boolean deleteFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }
        try {
            String fullPath = PROJECT_ROOT + filePath;
            File file = new File(fullPath);
            return file.delete();
        } catch (Exception e) {
            System.err.println("Failed to delete file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get full path for URL
     */
    public String getFullUrl(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return null;
        }
        return "/v1" + relativePath;
    }
}
