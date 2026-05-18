package com.superme.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class
AddCourseMultipartRequest {

    private String courseName;
    private String description;
    private String category;
    private String difficulty;
    private String format;
    private Integer duration;
    private Integer noOfLessons;
    private Integer totalCoins;
    private String status;
    private List<String> ageGroup = new ArrayList<>();
    private MultipartFile thumbnail;
    private String existingThumbnailUrl;
    private MultipartFile attachment;
    private String existingAttachmentUrl;
    private List<LessonMetadata> lessonsMetadata = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    public static class LessonMetadata {
        private Long id;
        private String lessonTitle;
        private String lessonDescription;
        private String format;
        private Integer coins;
        private Integer duration;
        private String content;
        private Integer lessonOrder;
    }
}