//package com.superme.controller;
//
//import com.superme.model.Avatar;
//import com.superme.model.User;
//import com.superme.repository.AvatarRepository;
//import com.superme.repository.UserRepository;
//import jakarta.servlet.http.HttpServletRequest;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/avatar")
//@CrossOrigin(origins = "*")
//public class AvatarController {// added by mohit kumar
//
//    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/avatars/";
//
//    @Autowired
//    private AvatarRepository avatarRepository;
//
//    @Autowired
//    private UserRepository userRepository;
//
//    // ✅ Upload Avatar
//    @PostMapping("/upload")
//    public ResponseEntity<?> uploadAvatar(
//            @RequestParam("file") MultipartFile file,
//            @RequestParam Long userId,
//            HttpServletRequest request) {
//
//        try {
//            if (file.isEmpty()) {
//                return ResponseEntity.badRequest().body("File is empty");
//            }
//
//            Path uploadPath = Paths.get(UPLOAD_DIR);
//            if (!Files.exists(uploadPath)) {
//                Files.createDirectories(uploadPath);
//            }
//
//            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
//            Path filePath = uploadPath.resolve(fileName);
//
//            Files.write(filePath, file.getBytes());
//
//            // ✅ store ONLY relative path in DB
//            String dbPath = "/uploads/avatars/" + fileName;
//
//            Avatar avatar = new Avatar();
//            avatar.setAvatarImageName(file.getOriginalFilename());
////            avatar.setAvatarImagePath(dbPath);
//
//            avatarRepository.save(avatar);
//
//            User user = userRepository.findById(userId)
//                    .orElseThrow(() -> new RuntimeException("User not found"));
//
//            user.setAvatar(avatar);
//            userRepository.save(user);
//
//            // ✅ build dynamic URL
//            String baseUrl = getBaseUrl(request);
//            String fullUrl = baseUrl + dbPath;
//
//            return ResponseEntity.ok(Map.of(
//                    "message", "Avatar uploaded successfully",
//                    "imageUrl", fullUrl
//            ));
//
//        } catch (Exception e) {
//            return ResponseEntity.internalServerError()
//                    .body("Upload failed: " + e.getMessage());
//        }
//    }
//    // ✅ Get Avatar by User ID
//    @GetMapping("/user/{userId}")
//    public ResponseEntity<?> getUserAvatar(
//            @PathVariable Long userId,
//            HttpServletRequest request) {
//
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new RuntimeException("User not found"));
//
//        Avatar avatar = user.getAvatar();
//
//        if (avatar == null) {
//            return ResponseEntity.ok(Map.of(
//                    "message", "No avatar found",
//                    "imageUrl", null
//            ));
//        }
//
//        String baseUrl = getBaseUrl(request);
////        String imageUrl = baseUrl + avatar.getAvatarImagePath();
//
//        return ResponseEntity.ok(Map.of(
//                "avatarId", avatar.getId(),
//                "imageUrl", imageUrl,
//                "name", avatar.getAvatarImageName()
//        ));
//    }
//
//    private String getBaseUrl(HttpServletRequest request) {
//        return request.getScheme() + "://" +
//                request.getServerName() +
//                (request.getServerPort() != 80 && request.getServerPort() != 443
//                        ? ":" + request.getServerPort()
//                        : "");
//    }
//}