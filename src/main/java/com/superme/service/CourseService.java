package com.superme.service;

import com.superme.config.FileStorageConfig;
import com.superme.dto.CourseWithProgressDTO;
import com.superme.dto.LessonWithProgressDTO;
import com.superme.enums.ActivityType;
import com.superme.enums.LessonStatus;
import com.superme.exception.BusinessException;
import com.superme.model.*;
import com.superme.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseService {

    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final CourseProgressRepository courseProgressRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final CoinTransactionRepository coinTransactionRepository;
    private final BadgeService badgeService;
    private final CoinService coinService;

    public List<CourseWithProgressDTO> getAllCoursesWithProgress(User user) {
        List<Course> courses = courseRepository.findAll();

        return courses.stream()
                .map(course -> getCourseWithProgress(course, user, true))
                .collect(Collectors.toList());
    }

    public CourseWithProgressDTO getCourseWithProgress(Long courseId, User user) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException("Course not found"));

        return getCourseWithProgress(course, user, true);
    }

    private CourseWithProgressDTO getCourseWithProgress(Course course, User user, boolean includeLessons) {
        Long userId = user.getId();

        // Get or create course progress
        CourseProgress courseProgress = courseProgressRepository
                .findByUserIdAndCourseId(userId, course.getId())
                .orElse(CourseProgress.builder()
                        .userId(userId)
                        .course(course)
                        .completed(false)
                        .completedLessons(0)
                        .completionPercentage(0.0)
                        .build());

        // Fix 1: Change method name to match repository
        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByLessonOrderAsc(course.getId());

        // Calculate completed lessons and get lesson progress
        List<LessonWithProgressDTO> lessonsWithProgress = includeLessons ?
                getLessonsWithProgress(lessons, userId) : null;

        // Count completed lessons
        Integer completedLessons = lessonsWithProgress != null ?
                (int) lessonsWithProgress.stream()
                        .filter(LessonWithProgressDTO::getIsCompleted)
                        .count() :
                courseProgressRepository.countCompletedLessonsByUserAndCourse(userId, course.getId());

        // Calculate completion percentage
        double completionPercentage = course.getNoOfLessons() > 0
                ? (completedLessons * 100.0) / course.getNoOfLessons()
                : 0.0;

        // Check if course is completed (all lessons completed)
        boolean isCourseCompleted = completedLessons >= course.getNoOfLessons();

        // Fix 2: Changed from getCompletedAt() to isCompleted()
        boolean needsUpdate = courseProgress.getId() == null ||
                !courseProgress.isCompleted() == isCourseCompleted ||
                !courseProgress.getCompletedLessons().equals(completedLessons);

        if (needsUpdate) {
            courseProgress.setCompleted(isCourseCompleted);
            courseProgress.setCompletedLessons(completedLessons);
            courseProgress.setCompletionPercentage(completionPercentage);

            if (isCourseCompleted && courseProgress.getCompletedAt() == null) {
                courseProgress.setCompletedAt(LocalDateTime.now());
            }

            if (courseProgress.getStartedAt() == null) {
                courseProgress.setStartedAt(LocalDateTime.now());
            }

            courseProgressRepository.save(courseProgress);
        }

        return CourseWithProgressDTO.builder()
                .id(course.getId())
                .courseName(course.getCourseName())
                .description(course.getDescription())
                .category(course.getCategory())
                .difficulty(course.getDifficulty())
                .noOfLessons(course.getNoOfLessons())
                .ageGroups(course.getAgeGroups())
                .duration(course.getDuration())
                .format(course.getFormat())
                .totalCoins(course.getTotalCoins())
//                .thumbnailUrl(course.getThumbnailUrl()) // target api in course
                .thumbnailUrl(buildFileUrl(course.getThumbnailUrl())) // target api in course
                .status(course.getStatus())
                .completedLessons(completedLessons)
                .completionPercentage(Math.round(completionPercentage * 100.0) / 100.0)
                .isCourseCompleted(isCourseCompleted)
                .startedAt(courseProgress.getStartedAt())
                .completedAt(courseProgress.getCompletedAt())
                .lessons(lessonsWithProgress)
                .build();
    }


    // commit to fix the commit history
    private List<LessonWithProgressDTO> getLessonsWithProgress(List<Lesson> lessons, Long userId) {
        return lessons.stream()
                // Fix 3: Use getLessonOrder() instead of getOrder()
                .sorted(Comparator.comparing(Lesson::getLessonOrder))
                .map(lesson -> getLessonWithProgress(lesson, userId))
                .collect(Collectors.toList());
    }




    @Autowired
    private FileStorageConfig fileStorageConfig;

    private String buildFileUrl(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return null;
        }

        // extract filename from /uploads/tempimg.png
        String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
        return fileStorageConfig.getBaseUrl() + "/v1/courses/download/" + fileName;
//        return fileStorageConfig.getBaseUrl() + "/api/files/download/" + fileName;
    }



    // Change from private to public
    public LessonWithProgressDTO getLessonWithProgress(Lesson lesson, Long userId) {
        // Get user's progress for this lesson
        LessonProgress lessonProgress = lessonProgressRepository
                .findByUserIdAndLessonId(userId, lesson.getId())
                .orElse(LessonProgress.builder()
                        .userId(userId)
                        .lesson(lesson)
                        .lessonStatus(LessonStatus.PENDING)
                        .completed(false)
                        .build());

        // Check if lesson should be locked (optional)
        boolean isLocked = shouldLessonBeLocked(lesson, userId);

        return LessonWithProgressDTO.builder()
                .id(lesson.getId())
                .title(lesson.getLessonTitle())
                .description(lesson.getLessonDescription())
                .duration(lesson.getDuration())
                .order(lesson.getLessonOrder())
//                .thumbnailUrl(lesson.getThumbnailUrl()) // target api inlesson
                .thumbnailUrl(buildFileUrl(lesson.getThumbnailUrl())) // target api in course
                .content(lesson.getContent())
                .lessonStatus(lessonProgress.getLessonStatus())
                .isCompleted(lessonProgress.isCompleted())
                .completedAt(lessonProgress.getCompletedAt())
                .timeSpentMinutes(lessonProgress.getTimeSpentMinutes())
                .lastAccessedAt(lessonProgress.getLastAccessedAt())
                .isLocked(isLocked)
                .build();
    }
    private boolean shouldLessonBeLocked(Lesson lesson, Long userId) {
        // Optional: Implement lesson locking logic
        // Return false if you don't need locking

        // Example: Check if previous lessons are completed
        Course course = lesson.getCourse();
        // Fix 1: Use correct method name
        List<Lesson> allLessons = lessonRepository.findByCourseIdOrderByLessonOrderAsc(course.getId());

        int currentIndex = -1;
        for (int i = 0; i < allLessons.size(); i++) {
            if (allLessons.get(i).getId().equals(lesson.getId())) {
                currentIndex = i;
                break;
            }
        }

        // First lesson is never locked
        if (currentIndex <= 0) {
            return false;
        }

        // Check previous lessons
        for (int i = 0; i < currentIndex; i++) {
            Lesson prevLesson = allLessons.get(i);
            LessonProgress progress = lessonProgressRepository
                    .findByUserIdAndLessonId(userId, prevLesson.getId())
                    .orElse(null);

            if (progress == null || !progress.isCompleted()) {
                return true;
            }
        }

        return false;
    }

    @Transactional
    public Lesson markLessonAsCompleted(Long lessonId, User user) {
        Long userId = user.getId();

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException("Lesson not found"));

        Course course = lesson.getCourse();

        // Check if lesson is locked (optional)
        if (shouldLessonBeLocked(lesson, userId)) {
            throw new BusinessException("Complete previous lessons first.");
        }

        // Check if already completed
        LessonProgress lessonProgress = lessonProgressRepository
                .findByUserIdAndLessonId(userId, lessonId)
                .orElse(LessonProgress.builder()
                        .userId(userId)
                        .lesson(lesson)
                        .lessonStatus(LessonStatus.PENDING)
                        .completed(false)
                        .build());

        if (!lessonProgress.isCompleted()) {
            // Mark lesson as completed
            lessonProgress.setCompleted(true);
            lessonProgress.setLessonStatus(LessonStatus.COMPLETED);
            lessonProgress.setCompletedAt(LocalDateTime.now());
            lessonProgress.setTimeSpentMinutes(lesson.getDuration());
            lessonProgressRepository.save(lessonProgress);

            // Update course progress
            updateCourseProgress(userId, course);

            // Award coins for lesson completion
            coinService.addCoins(user, lesson.getCoins(), "LESSON_COMPLETED", "Completed lesson: " + lesson.getLessonTitle());

            // ✅ SINGLE badge trigger (after persistence)
            badgeService.triggerBadgeUpdate(
                    user.getId(),
                    ActivityType.LESSON_COMPLETED,
                    lesson.getCoins(),
                    "CHALLENGE"
            );

            log.info(
                    "Triggered badge evaluation for user {} after completing Lesson {} (type: {})",
                    user.getId(),
                    lesson.getLessonTitle(),
                    ActivityType.LESSON_COMPLETED
            );
        }
        return lesson;
    }

    private void updateCourseProgress(Long userId, Course course) {
        CourseProgress courseProgress = courseProgressRepository
                .findByUserIdAndCourseId(userId, course.getId())
                .orElse(CourseProgress.builder()
                        .userId(userId)
                        .course(course)
                        .completed(false)
                        .completedLessons(0)
                        .completionPercentage(0.0)
                        .build());

        // Count completed lessons
        Integer completedLessons = courseProgressRepository
                .countCompletedLessonsByUserAndCourse(userId, course.getId());

        // Calculate percentage
        double completionPercentage = course.getNoOfLessons() > 0
                ? (completedLessons * 100.0) / course.getNoOfLessons()
                : 0.0;

        // Check if course is completed
        boolean isCourseCompleted = completedLessons >= course.getNoOfLessons();

        // Update progress
        courseProgress.setCompleted(isCourseCompleted);
        courseProgress.setCompletedLessons(completedLessons);
        courseProgress.setCompletionPercentage(completionPercentage);

        if (isCourseCompleted && courseProgress.getCompletedAt() == null) {
            courseProgress.setCompletedAt(LocalDateTime.now());

            // Award bonus coins for course completion
            awardCoinsForCourseCompletion(userId, course);
        }

        if (courseProgress.getStartedAt() == null) {
            courseProgress.setStartedAt(LocalDateTime.now());
        }

        courseProgressRepository.save(courseProgress);
    }

    private void awardCoinsForLessonCompletion(User user, Course course, Lesson lesson) {
        int coinsPerLesson = 10;
        user.setCoins(user.getCoins() + coinsPerLesson);
        userRepository.save(user);

        CoinTransaction transaction = CoinTransaction.builder()
                .user(user)
                .amount(coinsPerLesson)
                .type("LESSON_COMPLETED")
                .description("Completed lesson: " + lesson.getLessonTitle())
                .timestamp(LocalDateTime.now())
                .build();
        coinTransactionRepository.save(transaction);
    }

    private void awardCoinsForCourseCompletion(Long userId, Course course) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        int bonusCoins = 50;
        user.setCoins(user.getCoins() + bonusCoins);
        userRepository.save(user);

        CoinTransaction transaction = CoinTransaction.builder()
                .user(user)
                .amount(bonusCoins)
                .type("COURSE_COMPLETED")
                .description("Completed course: " + course.getCourseName())
                .timestamp(LocalDateTime.now())
                .build();
        coinTransactionRepository.save(transaction);
    }

    @Transactional
    public void updateLessonTimeSpent(Long lessonId, Long userId, Integer minutesSpent) {
        LessonProgress lessonProgress = lessonProgressRepository
                .findByUserIdAndLessonId(userId, lessonId)
                .orElse(LessonProgress.builder()
                        .userId(userId)
                        .lesson(lessonRepository.findById(lessonId)
                                .orElseThrow(() -> new BusinessException("Lesson not found")))
                        .completed(false)
                        .build());

        lessonProgress.setTimeSpentMinutes(
                lessonProgress.getTimeSpentMinutes() + minutesSpent
        );
        lessonProgressRepository.save(lessonProgress);
    }

    // Get user's in-progress courses
    public List<CourseWithProgressDTO> getInProgressCourses(User user) {
        Long userId = user.getId();
        List<CourseProgress> userProgress = courseProgressRepository.findByUserId(userId);

        return userProgress.stream()
                .filter(progress ->
                        progress.getCompletedLessons() > 0 &&
                                !progress.isCompleted()
                )
                .map(progress -> getCourseWithProgress(progress.getCourse(), user, true))
                .collect(Collectors.toList());
    }

    // Get user's completed courses
    public List<CourseWithProgressDTO> getCompletedCourses(User user) {
        Long userId = user.getId();
        List<CourseProgress> userProgress = courseProgressRepository.findByUserId(userId);

        return userProgress.stream()
                .filter(CourseProgress::isCompleted)
                .map(progress -> getCourseWithProgress(progress.getCourse(), user, true))
                .collect(Collectors.toList());
    }

    // Get courses not started yet
    public List<CourseWithProgressDTO> getNotStartedCourses(User user) {
        List<Course> allCourses = courseRepository.findAll();
        Long userId = user.getId();

        return allCourses.stream()
                .filter(course -> {
                    Optional<CourseProgress> progress = courseProgressRepository
                            .findByUserIdAndCourseId(userId, course.getId());
                    return progress.isEmpty() ||
                            (progress.get().getCompletedLessons() == 0 && !progress.get().isCompleted());
                })
                .map(course -> getCourseWithProgress(course, user, false))
                .collect(Collectors.toList());
    }

    // Add these methods for backward compatibility
    public Optional<Course> getCourse(Long courseId) {
        return courseRepository.findById(courseId);
    }

    public Optional<Lesson> getLesson(Long lessonId) {
        return lessonRepository.findById(lessonId);
    }

    // Keep this old method for backward compatibility
    public LessonProgress completeLesson(Long userId, Long lessonId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));
        Lesson lesson = markLessonAsCompleted(lessonId, user);

        // Return the lesson progress
        return lessonProgressRepository.findByUserIdAndLessonId(userId, lessonId)
                .orElseThrow(() -> new BusinessException("Lesson progress not found"));
    }

    // Keep this old method for backward compatibility
    public boolean isCourseCompleted(Long userId, Long courseId) {
        Optional<CourseProgress> progress = courseProgressRepository.findByUserIdAndCourseId(userId, courseId);
        return progress.isPresent() && progress.get().isCompleted();
    }


    @Transactional
    public LessonWithProgressDTO getLessonWithProgressAfterCompletion(Long lessonId, User user) {
        Long userId = user.getId();

        // First mark the lesson as completed
        Lesson lesson = markLessonAsCompleted(lessonId, user);

        // Then get the lesson with progress DTO
        return getLessonWithProgress(lesson, userId);
    }

    @Transactional
    public String updateLessonStatus(Long lessonId, LessonStatus status, User user) {
        Long userId = user.getId();

        LessonProgress lessonProgress = lessonProgressRepository
                .findByUserIdAndLessonId(userId, lessonId)
                .orElseThrow(() -> new BusinessException("Lesson progress not found"));

        lessonProgress.setLessonStatus(status);
        lessonProgressRepository.save(lessonProgress);
        return "Lesson status updated to " + status.toString();
    }
}