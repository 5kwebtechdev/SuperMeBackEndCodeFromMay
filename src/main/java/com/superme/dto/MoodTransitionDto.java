package com.superme.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoodTransitionDto {

    private Long userId;          // ID of the user (child/self)
    private String userName;
    private LocalDate date;

    private String fromMood;      // e.g., "Happy"
    private String toMood;        // e.g., "Very Happy"
    private Boolean parentVersion; // true if parent is viewing, false otherwise

    private String title;         // e.g., "Your child felt amazing today!"
    private String firstLine;     // e.g., "They were glowing with joy"
    private String secondLine;    // e.g., "Keep up the positivity!"
}
