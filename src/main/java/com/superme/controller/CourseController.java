package com.superme.controller;

import com.superme.config.FileStorageConfig;
import com.superme.dto.CourseWithProgressDTO;
import com.superme.dto.LessonWithProgressDTO;
import com.superme.enums.LessonStatus;
import com.superme.exception.BusinessException;
import com.superme.model.Course;
import com.superme.model.Lesson;
import com.superme.model.LessonProgress;
import com.superme.model.User;
import com.superme.service.CourseService;
import com.superme.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/courses")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.DELETE, RequestMethod.OPTIONS })
public class CourseController {
    @Autowired
    private CourseService courseService;

    @Autowired
    private UserService userService;

    // Get all courses with user progress
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CourseWithProgressDTO>> getAllCoursesWithProgress(Principal principal) {
        try {
            User user = userService.getUserFromPrincipal(principal);
            List<CourseWithProgressDTO> courses = courseService.getAllCoursesWithProgress(user);
            return ResponseEntity.ok(courses);
        } catch (Exception e) {
            throw new BusinessException("Error retrieving courses: " + e.getMessage());
        }
    }

    // Get single course with user progress and lessons
    @GetMapping("/{courseId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CourseWithProgressDTO> getCourseWithProgress(
            @PathVariable Long courseId,
            Principal principal) {
        try {
            User user = userService.getUserFromPrincipal(principal);
            CourseWithProgressDTO course = courseService.getCourseWithProgress(courseId, user);
            return ResponseEntity.ok(course);
        } catch (Exception e) {
            throw new BusinessException("Error retrieving course: " + e.getMessage());
        }
    }

    // Original endpoints (keep for backward compatibility if needed)
    @GetMapping("/basic/{courseId}")
    public ResponseEntity<Course> getCourseBasic(@PathVariable Long courseId) {
        try {
            return courseService.getCourse(courseId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            throw new BusinessException("Error retrieving course: " + e.getMessage());
        }
    }

    @GetMapping("/lessons/{lessonId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LessonWithProgressDTO> getLesson(@PathVariable Long lessonId, Principal principal) {
        try {
            User user = userService.getUserFromPrincipal(principal);
            Lesson lesson = courseService.getLesson(lessonId)
                    .orElseThrow(() -> new BusinessException("Lesson not found"));

            LessonWithProgressDTO lessonWithProgress = courseService.getLessonWithProgress(lesson, user.getId());
            return ResponseEntity.ok(lessonWithProgress);
        } catch (Exception e) {
            throw new BusinessException("Error retrieving lesson: " + e.getMessage());
        }
    }

    @PostMapping("/lessons/{lessonId}/complete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LessonWithProgressDTO> completeLesson(@PathVariable Long lessonId, Principal principal) {
        try {
            User user = userService.getUserFromPrincipal(principal);
            LessonWithProgressDTO lessonWithProgress = courseService.getLessonWithProgressAfterCompletion(lessonId, user);
            return ResponseEntity.ok(lessonWithProgress);
        } catch (Exception e) {
            throw new BusinessException("Error completing lesson: " + e.getMessage());
        }
    }

    @PostMapping("/lessons/update-status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateLessonStatus(@RequestParam Long lessonId,
                                                @RequestParam LessonStatus status,
                                                Principal principal) {
        try {
            User user = userService.getUserFromPrincipal(principal);
            String updateLessonStatus = courseService.updateLessonStatus(lessonId, status, user);
            return ResponseEntity.ok(updateLessonStatus);
        } catch (Exception e) {
            throw new BusinessException("Error updating lesson status: " + e.getMessage());
        }
    }



    // Track time spent on a lesson
    @PostMapping("/lessons/{lessonId}/track-time")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> trackLessonTime(
            @PathVariable Long lessonId,
            @RequestParam Integer minutesSpent,
            Principal principal) {
        try {
            User user = userService.getUserFromPrincipal(principal);
            courseService.updateLessonTimeSpent(lessonId, user.getId(), minutesSpent);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            throw new BusinessException("Error tracking lesson time: " + e.getMessage());
        }
    }

    // Get in-progress courses for the user
    @GetMapping("/in-progress")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CourseWithProgressDTO>> getInProgressCourses(Principal principal) {
        try {
            User user = userService.getUserFromPrincipal(principal);
            List<CourseWithProgressDTO> courses = courseService.getInProgressCourses(user);
            return ResponseEntity.ok(courses);
        } catch (Exception e) {
            throw new BusinessException("Error retrieving in-progress courses: " + e.getMessage());
        }
    }

    // Get completed courses for the user
    @GetMapping("/completed")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CourseWithProgressDTO>> getCompletedCourses(Principal principal) {
        try {
            User user = userService.getUserFromPrincipal(principal);
            List<CourseWithProgressDTO> courses = courseService.getCompletedCourses(user);
            return ResponseEntity.ok(courses);
        } catch (Exception e) {
            throw new BusinessException("Error retrieving completed courses: " + e.getMessage());
        }
    }

    // Get courses not started yet
    @GetMapping("/not-started")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CourseWithProgressDTO>> getNotStartedCourses(Principal principal) {
        try {
            User user = userService.getUserFromPrincipal(principal);
            List<CourseWithProgressDTO> courses = courseService.getNotStartedCourses(user);
            return ResponseEntity.ok(courses);
        } catch (Exception e) {
            throw new BusinessException("Error retrieving not-started courses: " + e.getMessage());
        }
    }

    @GetMapping("/{courseId}/completion-status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Boolean> isCourseCompleted(@PathVariable Long courseId, Principal principal) {
        try {
            User user = userService.getUserFromPrincipal(principal);

            // Use the old method for backward compatibility
            boolean completed = courseService.isCourseCompleted(user.getId(), courseId);

            return ResponseEntity.ok(completed);
        } catch (Exception e) {
            throw new BusinessException("Error checking course completion status: " + e.getMessage());
        }
    }
    // DTO for lesson completion response (keep for backward compatibility)
    public static class LessonCompleteResponse {
        private LessonProgress lessonProgress;
        private boolean courseCompleted;

        public LessonCompleteResponse() {}

        public LessonCompleteResponse(LessonProgress lessonProgress, boolean courseCompleted) {
            this.lessonProgress = lessonProgress;
            this.courseCompleted = courseCompleted;
        }

        public LessonProgress getLessonProgress() {
            return lessonProgress;
        }

        public void setLessonProgress(LessonProgress lessonProgress) {
            this.lessonProgress = lessonProgress;
        }

        public boolean isCourseCompleted() {
            return courseCompleted;
        }

        public void setCourseCompleted(boolean courseCompleted) {
            this.courseCompleted = courseCompleted;
        }
    }









    @Autowired
    private FileStorageConfig fileStorageConfig;

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {

        try {
            Path filePath = Paths.get(fileStorageConfig.getUploadDir())
                    .resolve(fileName)
                    .normalize();

            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                throw new RuntimeException("File not found");
            }

            String contentType = "image/png"; // default

            // optional: detect type dynamically
            try {
                contentType = Files.probeContentType(filePath);
            } catch (Exception ignored) {}

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            throw new RuntimeException("Error reading file", e);
        }
    }

}