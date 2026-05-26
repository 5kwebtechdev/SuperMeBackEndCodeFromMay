package com.superme.service;

import com.superme.dto.*;
import com.superme.enums.*;
import com.superme.exception.ResourceNotFoundException;
import com.superme.model.Course;
import com.superme.model.Lesson;
import com.superme.repository.CourseRepository;
import com.superme.repository.LessonRepository;

import com.superme.specification.CourseSpecification;
import jakarta.persistence.criteria.Predicate;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Base64;

/**
 * Admin Academic Service for managing courses and lessons.
 */
@Service
@Transactional
public class AdminAcademicService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Value("${file.base-url}")
    private String fileBaseUrl;

    // ============================================================================
    // COURSE CREATION AND MANAGEMENT
    // ============================================================================

    public Long createOrUpdateCourse(CreateCourseRequest request) {

        try {

            // 1️⃣ CREATE or UPDATE COURSE
            Course course;

            if (request.getId() != null) {
                course = courseRepository.findById(request.getId()).orElse(new Course());
            } else {
                course = new Course();
            }

            course.setCourseName(request.getCourseName());
            course.setDescription(request.getDescription());
            course.setCategory(request.getCategory().toString());
            course.setDuration(request.getDuration());
            course.setNoOfLessons(request.getNoOfLessons());
            course.setDifficulty(request.getDifficulty().toString());
            course.setFormat(request.getFormat());
            course.setTotalCoins(request.getTotalCoins());
            course.setAgeGroups(request.getAgeGroups() != null ? request.getAgeGroups() : new ArrayList<>());
            course.setThumbnailUrl(request.getThumbnailUrl());
            course.setStatus(request.getStatus());

            if (course.getId() == null) {
                course.setCreatedAt(LocalDateTime.now());
            }
            course.setUpdatedAt(LocalDateTime.now());

            Course savedCourse = courseRepository.save(course);


            // 2️⃣ HANDLE LESSONS (Create or Update individually)
            if (request.getLessons() != null && !request.getLessons().isEmpty()) {

                List<Lesson> lessonsToSave = new ArrayList<>();

                for (CreateCourseRequest.LessonRequest lessonReq : request.getLessons()) {

                    Lesson lesson;

                    // ✅ If ID exists, try to fetch existing
                    if (lessonReq.getId() != null) {
                        lesson = lessonRepository.findById(lessonReq.getId())
                                .orElse(new Lesson());
                    } else {
                        lesson = new Lesson();
                    }

                    // ✅ Set/update lesson fields
                    lesson.setLessonTitle(lessonReq.getLessonTitle());
                    lesson.setLessonDescription(lessonReq.getLessonDescription());
                    lesson.setFormat(lessonReq.getFormat());
                    lesson.setCoins(lessonReq.getCoins());
                    lesson.setDuration(lessonReq.getDuration());
                    lesson.setThumbnailUrl(lessonReq.getThumbnailUrl());
                    lesson.setContent(lessonReq.getContent());
                    lesson.setLessonOrder(lessonReq.getLessonOrder());
                    lesson.setCourse(savedCourse);

                    lessonsToSave.add(lesson);
                }

                // ✅ Save all lessons (Create/Update in bulk)
                lessonRepository.saveAll(lessonsToSave);
            }
            return savedCourse.getId();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create course: " + e.getMessage(), e);
        }
    }
    @Value("${file.base-url}")
    private String baseUrl;

    @Transactional
    public Long createCourseFromMultipart(AddCourseMultipartRequest request, MultipartHttpServletRequest multipartRequest) {
        try {
            Course course = new Course();
            course.setCourseName(request.getCourseName());
            course.setDescription(request.getDescription());
            course.setCategory(request.getCategory());
            course.setDifficulty(request.getDifficulty());
            course.setFormat(Format.valueOf(request.getFormat()));
            course.setDuration(request.getDuration());
            course.setNoOfLessons(request.getNoOfLessons());
            course.setTotalCoins(request.getTotalCoins());
            course.setStatus(Status.valueOf(request.getStatus()));

            if (request.getAgeGroup() != null && !request.getAgeGroup().isEmpty()) {
                List<AgeGroup> ageGroups = request.getAgeGroup().stream()
                        .map(AgeGroup::valueOf)
                        .collect(Collectors.toList());
                course.setAgeGroups(ageGroups);
            }

//            if (request.getThumbnail() != null && !request.getThumbnail().isEmpty()) {
//                String thumbnailUrl = saveFileLocally(request.getThumbnail());
//                course.setThumbnailUrl(thumbnailUrl);
//            }

            if (request.getThumbnail() != null && !request.getThumbnail().isEmpty()) {
                course.setThumbnailUrl(saveFileLocally(request.getThumbnail(), "Course"));
            }

            if (request.getAttachment() != null && !request.getAttachment().isEmpty()) {
                course.setAttachmentUrl(saveFileLocally(request.getAttachment(), "Course"));
            }

            course.setCreatedAt(LocalDateTime.now());
            course.setUpdatedAt(LocalDateTime.now());

            Course savedCourse = courseRepository.save(course);

            if (request.getLessonsMetadata() != null && !request.getLessonsMetadata().isEmpty()) {
                List<Lesson> lessonsToSave = new ArrayList<>();
                Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();

                for (int i = 0; i < request.getLessonsMetadata().size(); i++) {
                    AddCourseMultipartRequest.LessonMetadata meta = request.getLessonsMetadata().get(i);
                    Lesson lesson = new Lesson();

                    lesson.setLessonTitle(meta.getLessonTitle());
                    lesson.setLessonDescription(meta.getLessonDescription());
                    lesson.setFormat(Format.valueOf(meta.getFormat()));
                    lesson.setCoins(meta.getCoins());
                    lesson.setDuration(meta.getDuration());
                    lesson.setLessonOrder(meta.getLessonOrder());
                    lesson.setCourse(savedCourse);

//                    MultipartFile lessonThumbnail = fileMap.get("lessonThumbnail_" + i);
//                    if (lessonThumbnail != null && !lessonThumbnail.isEmpty()) {
//                        lesson.setThumbnailUrl(uploadThumbnailToS3(lessonThumbnail));
//                    }

                    MultipartFile lessonThumbnail = fileMap.get("lessonThumbnail_" + i);
                    if (lessonThumbnail != null && !lessonThumbnail.isEmpty()) {
                        String thumbnailPath = saveFileLocally(lessonThumbnail, "Course");
                        lesson.setThumbnailUrl(thumbnailPath);
                    }

//                    String content = meta.getContent() != null ? meta.getContent() : "";
//                    String imagePrefix = "lessonContentImage_" + i + "_";
//                    for (Map.Entry<String, MultipartFile> entry : fileMap.entrySet()) {
//                        if (entry.getKey().startsWith(imagePrefix)) {
//                            String idxStr = entry.getKey().substring(imagePrefix.length());
//                            try {
//                                int imgIdx = Integer.parseInt(idxStr);
//                                String imgUrl = uploadThumbnailToS3(entry.getValue());
//                                content = content.replace("[IMAGE_" + imgIdx + "]", "![image](" + imgUrl + ")");
//                            } catch (NumberFormatException ignored) {}
//                        }
//                    }


                    String content = meta.getContent() != null ? meta.getContent() : "";
                    String imagePrefix = "lessonContentImage_" + i + "_";
                    for (Map.Entry<String, MultipartFile> entry : fileMap.entrySet()) {
                        if (entry.getKey().startsWith(imagePrefix)) {
                            String idxStr = entry.getKey().substring(imagePrefix.length());
                            try {
                                int imgIdx = Integer.parseInt(idxStr);
                                String imgPath = saveFileLocally(entry.getValue(), "Course");
//                                content = content.replace("[IMAGE_" + imgIdx + "]", "![image](http://localhost:8080/download/image/" + imgPath.replace("\\", "/") + ")");
                                content = content.replace("[IMAGE_" + imgIdx + "]", "![image](" + baseUrl + "/" + imgPath + ")");
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                    lesson.setContent(content);

                    lessonsToSave.add(lesson);
                }
                lessonRepository.saveAll(lessonsToSave);
            }

            return savedCourse.getId();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create course: " + e.getMessage(), e);
        }
    }

//    public AdminCourseDetailResponse getCourseById(Long courseId) {
//        Course course = courseRepository.findById(courseId)
//                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
//
//        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByLessonOrderAsc(courseId);
//
//        AdminCourseDetailResponse response = new AdminCourseDetailResponse();
//        response.setId(course.getId());
//        response.setCourseName(course.getCourseName());
//        response.setDescription(course.getDescription());
//        response.setCategory(course.getCategory());
//        response.setDifficulty(course.getDifficulty());
//        response.setFormat(course.getFormat() != null ? course.getFormat().name() : null);
//        response.setDuration(course.getDuration());
//        response.setNoOfLessons(course.getNoOfLessons());
//        response.setAgeGroups(course.getAgeGroups() != null
//                ? course.getAgeGroups().stream().map(Enum::name).collect(Collectors.toList())
//                : new ArrayList<>());
//        response.setTotalCoins(course.getTotalCoins());
//        response.setStatus(course.getStatus() != null ? course.getStatus().name() : null);
//        response.setThumbnailUrl(buildFileUrl(course.getThumbnailUrl()));
//        response.setAttachmentUrl(buildFileUrl(course.getAttachmentUrl()));
//
//        List<AdminCourseDetailResponse.LessonDetail> lessonDetails = lessons.stream().map(lesson -> {
//            AdminCourseDetailResponse.LessonDetail ld = new AdminCourseDetailResponse.LessonDetail();
//            ld.setId(lesson.getId());
//            ld.setLessonTitle(lesson.getLessonTitle());
//            ld.setLessonDescription(lesson.getLessonDescription());
//            ld.setFormat(lesson.getFormat() != null ? lesson.getFormat().name() : null);
//            ld.setCoins(lesson.getCoins());
//            ld.setDuration(lesson.getDuration());
//            ld.setLessonOrder(lesson.getLessonOrder());
//            ld.setThumbnailUrl(buildFileUrl(lesson.getThumbnailUrl()));
//            ld.setContent(lesson.getContent());
//            ld.setContentBlocks(parseContentBlocks(lesson.getContent()));
//            return ld;
//        }).collect(Collectors.toList());
//
//        response.setLessons(lessonDetails);
//        return response;
//    }


    public AdminCourseDetailResponse getCourseById(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByLessonOrderAsc(courseId);

        AdminCourseDetailResponse response = new AdminCourseDetailResponse();
        response.setId(course.getId());
        response.setCourseName(course.getCourseName());
        response.setDescription(course.getDescription());
        response.setCategory(course.getCategory());
        response.setDifficulty(course.getDifficulty());
        response.setFormat(course.getFormat() != null ? course.getFormat().name() : null);
        response.setDuration(course.getDuration());
        response.setNoOfLessons(course.getNoOfLessons());
        response.setAgeGroups(course.getAgeGroups() != null
                ? course.getAgeGroups().stream().map(Enum::name).collect(Collectors.toList())
                : new ArrayList<>());
        response.setTotalCoins(course.getTotalCoins());
        response.setStatus(course.getStatus() != null ? course.getStatus().name() : null);
        response.setThumbnailUrl(buildFileUrl(course.getThumbnailUrl()));
        response.setAttachmentUrl(buildFileUrl(course.getAttachmentUrl()));

        List<AdminCourseDetailResponse.LessonDetail> lessonDetails = lessons.stream().map(lesson -> {
            AdminCourseDetailResponse.LessonDetail ld = new AdminCourseDetailResponse.LessonDetail();
            ld.setId(lesson.getId());
            ld.setLessonTitle(lesson.getLessonTitle());
            ld.setLessonDescription(lesson.getLessonDescription());
            ld.setFormat(lesson.getFormat() != null ? lesson.getFormat().name() : null);
            ld.setCoins(lesson.getCoins());
            ld.setDuration(lesson.getDuration());
            ld.setLessonOrder(lesson.getLessonOrder());
            ld.setThumbnailUrl(buildFileUrl(lesson.getThumbnailUrl()));
            ld.setContent(lesson.getContent());

            // Parse the HTML content into structured contentBlocks
            ld.setContentBlocks(parseContentBlocks(lesson.getContent()));

            return ld;
        }).collect(Collectors.toList());

        response.setLessons(lessonDetails);
        return response;
    }

    private String buildFileUrl(String path) {
        if (path == null || path.isBlank()) return null;
        if (path.startsWith("http://") || path.startsWith("https://")) return path;
        return fileBaseUrl + "/" + path;
    }

    private List<AdminCourseDetailResponse.ContentBlock> parseContentBlocks(String htmlContent) {
        List<AdminCourseDetailResponse.ContentBlock> contentBlocks = new ArrayList<>();

        if (htmlContent == null || htmlContent.isBlank()) {
            return contentBlocks;
        }

        // Remove the outer div if present
        String content = htmlContent.replaceAll("^<div class=\"lesson-content\">|</div>$", "");

        // Split by HTML tags and process each block
        // This is a simplified parser - you might need to adjust based on your exact HTML structure
        try {
            // Use JSoup for proper HTML parsing (add JSoup dependency to your project)
            org.jsoup.nodes.Document doc = Jsoup.parse(content);

            // Process all elements
            for (org.jsoup.nodes.Element element : doc.body().children()) {
                String tagName = element.tagName().toLowerCase();
                String text = element.text().trim();

                switch (tagName) {
                    case "h2":
                        contentBlocks.add(new AdminCourseDetailResponse.ContentBlock("heading", text));
                        break;
                    case "p":
                        contentBlocks.add(new AdminCourseDetailResponse.ContentBlock("paragraph", text));
                        break;
                    case "ul":
                        for (org.jsoup.nodes.Element li : element.select("li")) {
                            contentBlocks.add(new AdminCourseDetailResponse.ContentBlock("points", li.text().trim()));
                        }
                        break;
                    case "div":
                        if (element.hasClass("callout")) {
                            contentBlocks.add(new AdminCourseDetailResponse.ContentBlock("callout", text));
                        }
                        break;
                    case "img":
                        String src = element.attr("src");
                        // Extract the actual URL from the markdown-style link if present
                        if (src != null && src.startsWith("![image](") && src.endsWith(")")) {
                            src = src.substring("![image](".length(), src.length() - 1);
                        }
                        contentBlocks.add(new AdminCourseDetailResponse.ContentBlock("image", "", src));
                        break;
                    default:
                        // For other tags, just add as paragraph
                        if (!text.isBlank()) {
                            contentBlocks.add(new AdminCourseDetailResponse.ContentBlock("paragraph", text));
                        }
                }
            }
        } catch (Exception e) {
            // Fallback: if parsing fails, add the whole content as a paragraph
            contentBlocks.add(new AdminCourseDetailResponse.ContentBlock("paragraph", htmlContent));
        }

        return contentBlocks;
    }



//    private String buildFileUrl(String path) {
//        if (path == null || path.isBlank()) return null;
//        if (path.startsWith("http://") || path.startsWith("https://")) return path;
//        return fileBaseUrl + "/" + path;
//    }

//    private List<AdminCourseDetailResponse.ContentBlock> parseContentBlocks(String content) {
//        List<AdminCourseDetailResponse.ContentBlock> blocks = new ArrayList<>();
//        if (content == null || content.isBlank()) return blocks;
//
//        String[] paragraphs = content.split("\\n\\s*\\n");
//        for (String raw : paragraphs) {
//            String para = raw.strip();
//            if (para.isEmpty()) continue;
//
//            if (para.startsWith("![") && para.contains("](") && para.endsWith(")")) {
//                String imageUrl = para.substring(para.indexOf("](") + 2, para.length() - 1);
//                blocks.add(new AdminCourseDetailResponse.ContentBlock(imageUrl));
//            } else if (para.startsWith("# ")) {
//                blocks.add(new AdminCourseDetailResponse.ContentBlock("heading", para.substring(2).strip()));
//            } else if (para.startsWith("## ")) {
//                blocks.add(new AdminCourseDetailResponse.ContentBlock("heading", para.substring(3).strip()));
//            } else if (para.startsWith("### ")) {
//                blocks.add(new AdminCourseDetailResponse.ContentBlock("heading", para.substring(4).strip()));
//            } else if (para.startsWith("> ")) {
//                blocks.add(new AdminCourseDetailResponse.ContentBlock("callout", para.substring(2).strip()));
//            } else if (para.startsWith("- ") || para.startsWith("* ")) {
//                for (String line : para.split("\\n")) {
//                    String stripped = line.replaceFirst("^[-*]\\s+", "").strip();
//                    if (!stripped.isEmpty()) {
//                        blocks.add(new AdminCourseDetailResponse.ContentBlock("points", stripped));
//                    }
//                }
//            } else {
//                blocks.add(new AdminCourseDetailResponse.ContentBlock("paragraph", para));
//            }
//        }
//        return blocks;
//    }

    @Transactional
    public Long updateCourseFromMultipart(Long courseId, AddCourseMultipartRequest request,
                                          MultipartHttpServletRequest multipartRequest) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        course.setCourseName(request.getCourseName());
        course.setDescription(request.getDescription());
        course.setCategory(request.getCategory());
        course.setDifficulty(request.getDifficulty());
        course.setFormat(Format.valueOf(request.getFormat()));
        course.setDuration(request.getDuration());
        course.setNoOfLessons(request.getNoOfLessons());
        course.setTotalCoins(request.getTotalCoins());
        course.setStatus(parseStatus(request.getStatus()));
        course.setUpdatedAt(LocalDateTime.now());

        if (request.getAgeGroup() != null && !request.getAgeGroup().isEmpty()) {
            course.setAgeGroups(request.getAgeGroup().stream().map(AgeGroup::valueOf).collect(Collectors.toList()));
        }

        Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
        Map<String, String[]> paramMap = multipartRequest.getParameterMap();

        // Thumbnail: new file → swap; existing URL param → keep; neither → null
        MultipartFile thumbnailFile = fileMap.get("thumbnail");
        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            deleteLocalFile(course.getThumbnailUrl());
            course.setThumbnailUrl(saveFileLocally(thumbnailFile, "Course"));
        } else {
            String[] existingThumb = paramMap.get("existingThumbnailUrl");
            if (existingThumb != null && existingThumb.length > 0 && !existingThumb[0].isBlank()) {
                course.setThumbnailUrl(extractRelativePath(existingThumb[0]));
            }
        }

        // Attachment: new file → swap; existing URL param → keep; neither → null
        MultipartFile attachmentFile = fileMap.get("attachment");
        if (attachmentFile != null && !attachmentFile.isEmpty()) {
            deleteLocalFile(course.getAttachmentUrl());
            course.setAttachmentUrl(saveFileLocally(attachmentFile, "Course"));
        } else {
            String[] existingAttach = paramMap.get("existingAttachmentUrl");
            if (existingAttach != null && existingAttach.length > 0 && !existingAttach[0].isBlank()) {
                course.setAttachmentUrl(extractRelativePath(existingAttach[0]));
            }
        }

        Course savedCourse = courseRepository.save(course);

        if (request.getLessonsMetadata() != null && !request.getLessonsMetadata().isEmpty()) {
            Set<Long> incomingLessonIds = new HashSet<>();
            List<Lesson> lessonsToSave = new ArrayList<>();

            for (int i = 0; i < request.getLessonsMetadata().size(); i++) {
                AddCourseMultipartRequest.LessonMetadata meta = request.getLessonsMetadata().get(i);

                Lesson lesson = (meta.getId() != null)
                        ? lessonRepository.findById(meta.getId()).orElse(new Lesson())
                        : new Lesson();

                if (meta.getId() != null) incomingLessonIds.add(meta.getId());

                lesson.setLessonTitle(meta.getLessonTitle());
                lesson.setLessonDescription(meta.getLessonDescription());
                lesson.setFormat(Format.valueOf(meta.getFormat()));
                lesson.setCoins(meta.getCoins());
                lesson.setDuration(meta.getDuration());
                lesson.setLessonOrder(meta.getLessonOrder());
                lesson.setCourse(savedCourse);

                // Lesson thumbnail: new file → swap; existing param → keep
                MultipartFile lessonThumb = fileMap.get("lessonThumbnail_" + i);
                if (lessonThumb != null && !lessonThumb.isEmpty()) {
                    deleteLocalFile(lesson.getThumbnailUrl());
                    lesson.setThumbnailUrl(saveFileLocally(lessonThumb, "Course"));
                } else {
                    String[] existingThumb = paramMap.get("existingLessonThumbnailUrl_" + i);
                    if (existingThumb != null && existingThumb.length > 0 && !existingThumb[0].isBlank()) {
                        lesson.setThumbnailUrl(extractRelativePath(existingThumb[0]));
                    }
                }

                // Content images: replace [IMAGE_M] placeholders
                String content = meta.getContent() != null ? meta.getContent() : "";
                String imgPrefix = "lessonContentImage_" + i + "_";
                for (Map.Entry<String, MultipartFile> entry : fileMap.entrySet()) {
                    if (entry.getKey().startsWith(imgPrefix)) {
                        String idxStr = entry.getKey().substring(imgPrefix.length());
                        try {
                            int imgIdx = Integer.parseInt(idxStr);
                            // Delete old image if URL was provided
                            String[] oldUrls = paramMap.get("existingLessonContentImageUrl_" + i + "_" + imgIdx);
                            if (oldUrls != null && oldUrls.length > 0) {
                                deleteLocalFile(oldUrls[0]);
                            }
                            String newPath = saveFileLocally(entry.getValue(), "Course");
                            String newUrl = fileBaseUrl + "/" + newPath;
                            content = content.replace("[IMAGE_" + imgIdx + "]", newUrl);
                        } catch (NumberFormatException ignored) {}
                    }
                }
                // For unchanged content images, replace placeholder with existing URL
                for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
                    if (entry.getKey().startsWith("existingLessonContentImageUrl_" + i + "_")) {
                        String suffix = entry.getKey().substring(("existingLessonContentImageUrl_" + i + "_").length());
                        try {
                            int imgIdx = Integer.parseInt(suffix);
                            String existingUrl = entry.getValue()[0];
                            // Only replace if not already replaced by a new file
                            if (!fileMap.containsKey("lessonContentImage_" + i + "_" + imgIdx)
                                    && content.contains("[IMAGE_" + imgIdx + "]")) {
                                content = content.replace("[IMAGE_" + imgIdx + "]", existingUrl);
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
                lesson.setContent(content);
                lessonsToSave.add(lesson);
            }

            lessonRepository.saveAll(lessonsToSave);

            // Delete lessons removed from the request
            List<Lesson> existingLessons = lessonRepository.findByCourseIdOrderByLessonOrderAsc(courseId);
            for (Lesson existing : existingLessons) {
                if (existing.getId() != null && !incomingLessonIds.contains(existing.getId())) {
                    lessonRepository.delete(existing);
                }
            }
        }

        return savedCourse.getId();
    }

    private Status parseStatus(String value) {
        if (value == null) return Status.DRAFT;
        try {
            return Status.valueOf(value);
        } catch (IllegalArgumentException e) {
            // Map common aliases
            if ("PENDING".equalsIgnoreCase(value)) return Status.VERIFICATION_PENDING;
            return Status.DRAFT;
        }
    }

    private String extractRelativePath(String urlOrPath) {
        if (urlOrPath == null || urlOrPath.isBlank()) return null;
        if (!urlOrPath.startsWith("http://") && !urlOrPath.startsWith("https://")) return urlOrPath;
        try {
            String path = new java.net.URL(urlOrPath).getPath();
            if (path.startsWith("/")) path = path.substring(1);
            if (path.startsWith("download/image/")) path = path.substring("download/image/".length());
            return path;
        } catch (Exception e) {
            return urlOrPath;
        }
    }

    private void deleteLocalFile(String pathOrUrl) {
        if (pathOrUrl == null || pathOrUrl.isBlank()) return;
        try {
            String relativePath = extractRelativePath(pathOrUrl);
            if (relativePath == null || relativePath.isBlank()) return;
            Path filePath = Paths.get(uploadDir).resolve(relativePath);
            Files.deleteIfExists(filePath);
        } catch (Exception ignored) {}
    }

    public Course sendCourseForVerification(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found with id: " + courseId));

        if (course.getStatus() == Status.PUBLISHED) {
            throw new IllegalStateException("Course is already published.");
        }

        course.setStatus(Status.PUBLISHED);
        return courseRepository.save(course);
    }

    public void saveCourseAsDraft(Long courseId) {
        try {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("Course not found with id: " + courseId));

            course.setStatus(Status.DRAFT);
            courseRepository.save(course);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save course as draft: " + e.getMessage(), e);
        }
    }




    @Value("${file.upload-dir}")
    private String uploadDir;

    public String saveFileLocally(MultipartFile file, String subfolder) {
        try {
//            String uploadDir = env.getProperty("file.upload-dir");
            Path uploadPath = Paths.get(uploadDir, subfolder);

            // Create directory if it doesn't exist
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String filename = System.currentTimeMillis() + "_" + UUID.randomUUID().toString() +
                    Objects.requireNonNull(file.getOriginalFilename()).substring(file.getOriginalFilename().lastIndexOf("."));

            // Save file
            Path filePath = uploadPath.resolve(filename);
            file.transferTo(filePath);

             return Paths.get(subfolder, filename).toString().replace("\\", "/");
        } catch (Exception e) {
            throw new RuntimeException("Failed to save file locally: " + e.getMessage(), e);
        }
    }






















    public List<Map<String, String>> getCategoryOptions() {
        return Arrays.stream(CourseCategory.values())
                .map(category -> {
                    Map<String, String> option = new HashMap<>();
                    option.put("value", category.name());
                    option.put("label", category.getDisplayName());
                    return option;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, String>> getDifficultyOptions() {
        return Arrays.stream(CourseDifficulty.values())
                .map(difficulty -> {
                    Map<String, String> option = new HashMap<>();
                    option.put("value", difficulty.name());
                    option.put("label", difficulty.getDisplayName());
                    return option;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, String>> getFormatOptions() {
        return Arrays.stream(Format.values())
                .map(format -> {
                    Map<String, String> option = new HashMap<>();
                    option.put("value", format.name());
                    option.put("label", format.getDisplayName());
                    return option;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, String>> getAgeGroupOptions() {
        return Arrays.stream(AgeGroup.values())
                .map(ageGroup -> {
                    Map<String, String> option = new HashMap<>();
                    option.put("value", ageGroup.name());
                    option.put("label", ageGroup.getDisplayName());
                    return option;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, String>> getStatusOptions() {
        return Arrays.stream(Status.values())
                .map(status -> {
                    Map<String, String> option = new HashMap<>();
                    option.put("value", status.name());
                    option.put("label", status.getDisplayName());
                    return option;
                })
                .collect(Collectors.toList());
    }

    private String formatCategoryLabel(String category) {
        switch (category) {
            case "MANAGING_MONEY":
                return "Managing Money";
            case "COMMUNICATION":
                return "Communication";
            case "MEDIATION":
                return "Mediation";
            case "CULTURAL_VALUES":
                return "Cultural Values";
            default:
                return category;
        }
    }

    private String formatLabel(String value) {
        return value.charAt(0) + value.substring(1).toLowerCase().replace("_", " ");
    }

    // ============================================================================
    // ADMIN OVERVIEW FUNCTIONALITY
    // ============================================================================

    public Map<String, Object> getAcademicOverview(String search, String statusStr, String difficulty, String format, int page, int size) {
        // Parse status string case-insensitively (frontend sends "published", "draft" etc.)
        Status status = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                status = Status.valueOf(statusStr.trim().toUpperCase().replace(" ", "_"));
            } catch (IllegalArgumentException ignored) {}
        }

        // Fetch only active (non-deleted) courses
        List<Course> activeCourses = courseRepository.findByEnabled(true);

        // Convert to DTOs and apply all filters
        final Status finalStatus = status;
        List<AdminCourseDTO> filteredCourses = activeCourses.stream()
                .map(this::convertToAdminCourseDTO)
                .filter(c -> c.matchesSearchTerm(search))
                .filter(c -> c.matchesFilters(difficulty, null, finalStatus, null))
                .filter(c -> c.matchesFormat(format))
                .collect(Collectors.toList());

        // Page-based pagination
        int total = filteredCourses.size();
        int fromIndex = Math.max(0, page * size);
        int toIndex = Math.min(total, fromIndex + size);
        List<AdminCourseDTO> paginatedCourses = fromIndex < total
                ? filteredCourses.subList(fromIndex, toIndex)
                : new ArrayList<>();

        // Statistics (all counts exclude deleted courses)
        AcademicStatistics stats = getAcademicOverviewStatistics();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("courses", paginatedCourses);
        response.put("totalElements", total);
        response.put("page", page);
        response.put("size", size);
        response.put("totalPages", size > 0 ? (int) Math.ceil((double) total / size) : 0);
        response.put("statistics", stats);
        return response;
    }

    public Map<String, Object> searchCourses(
            String courseName,
            String description,
            String category,
            String difficulty,
            String ageGroup,
            String format,
            String status,
            Integer duration,
            Integer totalCoins,
            int page,
            int size
    ) {
        var spec = CourseSpecification.filterCourses(
                courseName, description, category, difficulty,
                ageGroup, format, status, duration, totalCoins
        );

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Course> coursePage = courseRepository.findAll(spec, pageable);

        // Get statistics
        AcademicStatistics statistics = getAcademicOverviewStatistics();


        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", coursePage.getContent());
        response.put("page", coursePage.getNumber());
        response.put("size", coursePage.getSize());
        response.put("totalElements", coursePage.getTotalElements());
        response.put("totalPages", coursePage.getTotalPages());
        response.put("statistics", statistics);

        return response;
    }

    public AcademicStatistics getAcademicOverviewStatistics() {
        AcademicStatistics stats = new AcademicStatistics();

        try {
            // Total courses (exclude soft-deleted)
            Long totalCourses = courseRepository.countByEnabled(true);
            stats.setTotalCourses(totalCourses);

            // Total lessons
            Long totalLessons = lessonRepository.count();
            stats.setTotalLessons(totalLessons);

            // Total categories (distinct)
            try {
                List<String> distinctCategories = courseRepository.findDistinctCategories();
                stats.setTotalCategories((long) distinctCategories.size());
            } catch (Exception e) {
                // Fallback to counting enum values
                stats.setTotalCategories((long) CourseCategory.values().length);
            }

            // Average duration
            try {
                Double averageDuration = courseRepository.findAverageDuration();
                if (averageDuration != null) {
                    stats.setAverageDuration(Math.round(averageDuration * 100.0) / 100.0);
                } else {
                    stats.setAverageDuration(0.0);
                }
            } catch (Exception e) {
                // Fallback calculation
                List<Course> allCourses = courseRepository.findAll();
                Double averageDuration = allCourses.stream()
                        .mapToInt(Course::getDuration)
                        .average()
                        .orElse(0.0);
                stats.setAverageDuration(Math.round(averageDuration * 100.0) / 100.0);
            }

            // Courses in draft (exclude soft-deleted)
            try {
                Long coursesInDraft = courseRepository.countByStatusAndEnabled(Status.DRAFT, true);
                stats.setCoursesInDraft(coursesInDraft);
            } catch (Exception e) {
                Long coursesInDraft = courseRepository.findByEnabled(true).stream()
                        .filter(c -> c.getStatus() == Status.DRAFT).count();
                stats.setCoursesInDraft(coursesInDraft);
            }

            // Courses published (approved)
            try {
                Long coursesPublished = courseRepository.countByStatus(Status.APPROVED);
                stats.setCoursesPublished(coursesPublished);
            } catch (Exception e) {
                // Fallback counting
                Long coursesPublished = courseRepository.findByStatus(Status.APPROVED).stream().count();
                stats.setCoursesPublished(coursesPublished);
            }

            // Courses pending
            try {
                Long coursesPending = courseRepository.countByStatus(Status.VERIFICATION_PENDING);
                stats.setCoursesPending(coursesPending);
            } catch (Exception e) {
                // Fallback counting
                Long coursesPending = courseRepository.findByStatus(Status.VERIFICATION_PENDING).stream()
                        .count();
                stats.setCoursesPending(coursesPending);
            }

        } catch (Exception e) {
            // Set default values on error
            stats.setTotalCourses(0L);
            stats.setTotalLessons(0L);
            stats.setTotalCategories(4L); // Number of categories in enum
            stats.setAverageDuration(0.0);
            stats.setCoursesInDraft(0L);
            stats.setCoursesPublished(0L);
            stats.setCoursesPending(0L);
        }

        return stats;
    }

    public Map<String, Object> getAcademicStatistics() {
        AcademicStatistics stats = getAcademicOverviewStatistics();

        Map<String, Object> result = new HashMap<>();
        result.put("totalCourses", stats.getTotalCourses());
        result.put("totalLessons", stats.getTotalLessons());
        result.put("totalCategories", stats.getTotalCategories());
        result.put("averageDuration", stats.getAverageDuration());
        result.put("coursesInDraft", stats.getCoursesInDraft());
        result.put("coursesPublished", stats.getCoursesPublished());
        result.put("coursesPending", stats.getCoursesPending());

        return result;
    }

    private AdminCourseDTO convertToAdminCourseDTO(Course course) {
        AdminCourseDTO dto = new AdminCourseDTO();
        dto.setId(course.getId());
        dto.setCourseName(course.getCourseName());
        dto.setDescription(course.getDescription());
        dto.setDuration(course.getDuration());
        dto.setAgeGroups(course.getAgeGroups());
        dto.setNoOfLessons(course.getNoOfLessons());
        dto.setDifficulty(formatLabel(course.getDifficulty().toString()));
        dto.setFormat(formatLabel(course.getFormat().name()));
        dto.setStatus(course.getStatus());
        dto.setCreatedAt(course.getCreatedAt());
        dto.setLastUpdated(course.getCreatedAt()); // Set to createdAt for now
        dto.setCategory(formatCategoryLabel(course.getCategory()));
        dto.setThumbnailUrl(course.getThumbnailUrl());
        dto.setAttachmentUrl(course.getAttachmentUrl());
        dto.setEnabled(course.isEnabled());
        dto.setTotalCoins(course.getTotalCoins());

        return dto;
    }

    // ============================================================================
    // COURSE LISTING AND STATISTICS
    // ============================================================================

    public List<Map<String, Object>> getAllCourses() {
        try {
            List<Course> courses = courseRepository.findAll();

            return courses.stream().map(course -> {
                Map<String, Object> courseData = new HashMap<>();
                courseData.put("id", course.getId());
                courseData.put("courseName", course.getCourseName());
                courseData.put("description", course.getDescription());
                courseData.put("category", formatCategoryLabel(course.getCategory()));
                courseData.put("duration", course.getDuration());
                courseData.put("noOfLessons", course.getNoOfLessons());
                courseData.put("difficulty", formatLabel(course.getDifficulty().toString()));
                courseData.put("format", formatLabel(course.getFormat().name()));
                courseData.put("ageGroups", course.getAgeGroups());
                courseData.put("thumbnailUrl", course.getThumbnailUrl());
                courseData.put("status", course.getStatus().name());
                courseData.put("createdAt", course.getCreatedAt());

                // Get lesson count safely
                try {
                    Long lessonCount = lessonRepository.countLessonsByCourseId(course.getId());
                    courseData.put("actualLessonCount", lessonCount);
                } catch (Exception e) {
                    courseData.put("actualLessonCount", 0L);
                }

                return courseData;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get all courses: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> getCourseStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            AcademicStatistics academicStats = getAcademicOverviewStatistics();

            stats.put("totalCourses", academicStats.getTotalCourses());
            stats.put("totalLessons", academicStats.getTotalLessons());
            stats.put("totalCategories", academicStats.getTotalCategories());
            stats.put("averageDuration", academicStats.getAverageDuration());
            stats.put("coursesInDraft", academicStats.getCoursesInDraft());
            stats.put("coursesPublished", academicStats.getCoursesPublished());
            stats.put("coursesPending", academicStats.getCoursesPending());
            stats.put("success", true);
        } catch (Exception e) {
            stats.put("success", false);
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    @Transactional
    public List<Long> createOrUpdateCourses(List<CreateCourseRequest> requests) {
        try {
            if (requests == null || requests.isEmpty()) {
                throw new IllegalArgumentException("Course list cannot be empty");
            }

            List<Long> courseIds = new ArrayList<>();

            for (CreateCourseRequest req : requests) {
                Long id = createOrUpdateCourse(req); // ✅ Reuse existing logic
                courseIds.add(id);
            }

            return courseIds;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create/update courses: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Course updateCourseStatus(StatusUpdateRequest request) {
        Course course = courseRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Course with ID " + request.getId() + " not found"));

        course.setStatus(request.getStatus());
        return courseRepository.save(course);
    }

    public void softDeleteCourse(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course with ID " + courseId + " not found"));

        if (!course.isEnabled()) {
            throw new IllegalStateException("Course is already deleted");
        }

        course.setEnabled(false);
        course.setUpdatedAt(LocalDateTime.now());
        courseRepository.save(course);
    }




}
