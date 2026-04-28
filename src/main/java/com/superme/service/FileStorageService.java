package com.superme.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    public String saveFile(Long tutorId, MultipartFile file, String type) {
        try {
            String folderPath = uploadDir + "/" + tutorId + "/" + type;
            Path folder = Paths.get(folderPath);

            if (!Files.exists(folder)) {
                Files.createDirectories(folder);
            }

            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = folder.resolve(fileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return filePath.toString(); // store this in DB

        } catch (IOException e) {
            throw new RuntimeException("File upload failed", e);
        }
    }
}