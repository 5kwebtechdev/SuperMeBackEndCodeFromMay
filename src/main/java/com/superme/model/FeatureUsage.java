package com.superme.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

@Entity
@Getter
@Setter
@Table(name = "feature_usage")
public class FeatureUsage {

    @Id
    private Long userId;

    private int habitsCount;
    private int tasksCount;
    private int quizCount;
    private int puzzleCount;
    private int articleCount;
    private int lessonCount;

    public int minUsage() {
        return Arrays.stream(new int[]{
                habitsCount,
                tasksCount,
                quizCount,
                puzzleCount,
                articleCount,
                lessonCount
        }).min().orElse(0);
    }

}

