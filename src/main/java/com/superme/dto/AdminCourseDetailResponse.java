//package com.superme.dto;
//
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//
//import java.util.List;
//
//@Getter
//@Setter
//@NoArgsConstructor
//public class AdminCourseDetailResponse {
//
//    private Long id;
//    private String courseName;
//    private String description;
//    private String category;
//    private String difficulty;
//    private String format;
//    private Integer duration;
//    private Integer noOfLessons;
//    private List<String> ageGroups;
//    private Integer totalCoins;
//    private String status;
//    private String thumbnailUrl;
//    private String attachmentUrl;
//    private List<LessonDetail> lessons;
//
//    @Getter
//    @Setter
//    @NoArgsConstructor
//    public static class LessonDetail {
//        private Long id;
//        private String lessonTitle;
//        private String lessonDescription;
//        private String format;
//        private Integer coins;
//        private Integer duration;
//        private Integer lessonOrder;
//        private String thumbnailUrl;
//        private String content;
//        private List<ContentBlock> contentBlocks;
//    }
//
//    @Getter
//    @Setter
//    @NoArgsConstructor
//    public static class ContentBlock {
//        private String type;
//        private String content;
//        private String imageUrl;
//
//        public ContentBlock(String type, String content) {
//            this.type = type;
//            this.content = content;
//        }
//
//        public ContentBlock(String imageUrl) {
//            this.type = "image";
//            this.content = "";
//            this.imageUrl = imageUrl;
//        }
//    }
//}


package com.superme.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class AdminCourseDetailResponse {
    private Long id;
    private String courseName;
    private String description;
    private String category;
    private String difficulty;
    private String format;
    private Integer duration;
    private Integer noOfLessons;
    private List<String> ageGroups;
    private Integer totalCoins;
    private String status;
    private String thumbnailUrl;
    private String attachmentUrl;
    private List<LessonDetail> lessons;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class LessonDetail {
        private Long id;
        private String lessonTitle;
        private String lessonDescription;
        private String format;
        private Integer coins;
        private Integer duration;
        private Integer lessonOrder;
        private String thumbnailUrl;
        private String content;
        private List<ContentBlock> contentBlocks;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ContentBlock {
        private String type; // "heading", "paragraph", "points", "callout", "image"
        private String content;
        private String imageUrl;

        public ContentBlock(String type, String content) {
            this.type = type;
            this.content = content;
        }

        public ContentBlock(String type, String content, String imageUrl) {
            this.type = type;
            this.content = content;
            this.imageUrl = imageUrl;
        }
    }
}