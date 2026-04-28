package com.superme.model;

import com.superme.enums.Category;
import com.superme.enums.Difficulty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Entity
@Table(name = "trophies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trophy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "trophy_count", nullable = false)
    private Integer trophyCount;

    @Column(name = "earned_date", nullable = false)
    private LocalDateTime earnedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false)
    private Difficulty difficulty;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "challenge_id")
    private String challengeId;

    @Column(name = "category_icon_url")
    private String categoryIconUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (earnedDate == null) {
            earnedDate = LocalDateTime.now();
        }
    }
}