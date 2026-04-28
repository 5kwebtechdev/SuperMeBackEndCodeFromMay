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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
            course.setAgeGroup(request.getAgeGroup());
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


    public void sendCourseForVerification(Long courseId) {
        try {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("Course not found with id: " + courseId));

            course.setStatus(Status.VERIFICATION_PENDING);
            courseRepository.save(course);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send course for verification: " + e.getMessage(), e);
        }
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

    // ============================================================================
    // S3 BASE64 IMAGE UPLOAD SERVICE
    // ============================================================================

    public String uploadThumbnailToS3(MultipartFile file) {
        try {
            // Validate file
            if (file.isEmpty()) {
                throw new IllegalArgumentException("File is empty");
            }

            // Validate file type (Images only)
            String contentType = file.getContentType();
            if (!isValidImageType(contentType)) {
                throw new IllegalArgumentException("Only image files (JPEG, PNG, GIF, WebP) are accepted");
            }

            // Validate file size (5MB max for images)
            long maxSize = 5 * 1024 * 1024; // 5MB in bytes
            if (file.getSize() > maxSize) {
                throw new IllegalArgumentException("File size exceeds 5MB limit");
            }

            // Convert to Base64
            String base64Image = convertToBase64(file);

            // Upload to S3 and get URL
            String s3Url = uploadBase64ImageToS3(base64Image, file.getOriginalFilename(), contentType);

            return s3Url;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload thumbnail: " + e.getMessage(), e);
        }
    }

    public String uploadBase64ImageToS3(String base64Image, String originalFilename, String contentType) {
        try {
            // Generate unique filename
            String extension = getFileExtension(originalFilename);
            String filename = "academic/thumbnails/" + System.currentTimeMillis() + "_" +
                    UUID.randomUUID().toString() + extension;

            // Decode base64 to bytes
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);

            // TODO: Implement actual S3 upload logic here
            // This is where you would use AWS SDK to upload the image bytes to S3
            // For now, return a mock S3 URL
            String s3BaseUrl = "https://your-bucket.s3.amazonaws.com/";
            return s3BaseUrl + filename;

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload base64 image to S3: " + e.getMessage(), e);
        }
    }

    public String convertToBase64(MultipartFile file) {
        try {
            byte[] fileBytes = file.getBytes();
            return Base64.getEncoder().encodeToString(fileBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert image to Base64: " + e.getMessage(), e);
        }
    }

    public String uploadBase64StringToS3(String base64String, String filename) {
        try {
            // Validate base64 string
            if (base64String == null || base64String.isEmpty()) {
                throw new IllegalArgumentException("Base64 string is empty");
            }

            // Remove data URL prefix if present (data:image/jpeg;base64,)
            String cleanBase64 = base64String;
            if (base64String.contains(",")) {
                cleanBase64 = base64String.split(",")[1];
            }

            // Determine content type from data URL
            String contentType = "image/jpeg"; // default
            if (base64String.startsWith("data:image/")) {
                String dataUrl = base64String.split(",")[0];
                if (dataUrl.contains("image/png"))
                    contentType = "image/png";
                else if (dataUrl.contains("image/gif"))
                    contentType = "image/gif";
                else if (dataUrl.contains("image/webp"))
                    contentType = "image/webp";
            }

            // Generate unique filename if not provided
            if (filename == null || filename.isEmpty()) {
                String extension = contentType.equals("image/png") ? ".png"
                        : contentType.equals("image/gif") ? ".gif"
                                : contentType.equals("image/webp") ? ".webp" : ".jpg";
                filename = "thumbnail_" + System.currentTimeMillis() + extension;
            }

            // Upload to S3
            return uploadBase64ImageToS3(cleanBase64, filename, contentType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload base64 string: " + e.getMessage(), e);
        }
    }

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================

    private boolean isValidImageType(String contentType) {
        return contentType != null && (contentType.equals("image/jpeg") ||
                contentType.equals("image/jpg") ||
                contentType.equals("image/png") ||
                contentType.equals("image/gif") ||
                contentType.equals("image/webp"));
    }

    private String getFileExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf("."));
        }
        return ".jpg"; // Default extension
    }

    // ============================================================================
    // DROPDOWN OPTIONS
    // ============================================================================

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

    public AdminAcademicOverviewResponse getAcademicOverview(String search, String difficulty, AgeGroup ageGroup,
                                                             Status status, String category, int limit, int offset, boolean enabled) {
        try {
            List<Course> allCourses = courseRepository.findAll();

            // Convert to DTOs
            List<AdminCourseDTO> courseDTOs = allCourses.stream()
                    .map(this::convertToAdminCourseDTO)
                    .collect(Collectors.toList());

            // Apply filters
            List<AdminCourseDTO> filteredCourses = courseDTOs.stream()
                    .filter(course -> course.matchesSearchTerm(search))
                    .filter(course -> course.matchesFilters(difficulty, ageGroup, status, category))
                    .filter(course -> course.isEnabled() == enabled) // ✅ Enabled filter added
                    .collect(Collectors.toList());

            // Apply pagination
            int start = Math.max(0, offset);
            int end = Math.min(filteredCourses.size(), start + limit);
            List<AdminCourseDTO> paginatedCourses = start < filteredCourses.size() ? filteredCourses.subList(start, end)
                    : new ArrayList<>();

            // Get statistics
            AcademicStatistics stats = getAcademicOverviewStatistics();

            // Create filter criteria
            AdminAcademicOverviewResponse.AcademicFilterCriteria criteria = new AdminAcademicOverviewResponse.AcademicFilterCriteria();
            criteria.setSearchTerm(search);
            criteria.setDifficulty(difficulty);
            criteria.setAgeGroup(ageGroup);
            criteria.setStatus(status);
            criteria.setCategory(category);

            // Build response
            AdminAcademicOverviewResponse response = new AdminAcademicOverviewResponse(paginatedCourses, stats,
                    criteria);
            response.setTotalCount(allCourses.size());
            response.setFilteredCount(filteredCourses.size());
            response.setHasMore((offset + limit) < filteredCourses.size());

            return response;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get academic overview: " + e.getMessage(), e);
        }
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
            // Total courses
            Long totalCourses = courseRepository.count();
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

            // Courses in draft
            try {
                Long coursesInDraft = courseRepository.countByStatus(Status.DRAFT);
                stats.setCoursesInDraft(coursesInDraft);
            } catch (Exception e) {
                // Fallback counting
                Long coursesInDraft = courseRepository.findByStatus(Status.DRAFT).stream().count();
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
        dto.setAgeGroup(course.getAgeGroup());
        dto.setNoOfLessons(course.getNoOfLessons());
        dto.setDifficulty(formatLabel(course.getDifficulty().toString()));
        dto.setFormat(formatLabel(course.getFormat().name()));
        dto.setStatus(course.getStatus());
        dto.setCreatedAt(course.getCreatedAt());
        dto.setLastUpdated(course.getCreatedAt()); // Set to createdAt for now
        dto.setCategory(formatCategoryLabel(course.getCategory()));
        dto.setThumbnailUrl(course.getThumbnailUrl());
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
                courseData.put("ageGroup", course.getAgeGroup());
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
