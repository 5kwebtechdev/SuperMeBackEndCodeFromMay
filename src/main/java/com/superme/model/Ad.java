package com.superme.model;

import com.superme.enums.AdStatus;
import com.superme.enums.Priority;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.superme.enums.*;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ads")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ad_name", nullable = false)
    private String adName;

    @Column(name = "ad_description", length = 500)
    private String adDescription;

    @Column(name = "banner_image_url")
    private String bannerImageUrl;

    @Column(name = "banner_image_s3_key")
    private String bannerImageS3Key;

    @Enumerated(EnumType.STRING)
    @Column(name = "ad_type", nullable = false)
    private AdType adType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_screen", nullable = false)
    private TargetScreen targetScreen;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_audience", nullable = false)
    private AgeGroup targetAudience;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "click_action_type", nullable = false)
    private ClickActionType clickActionType;

    @Column(name = "external_url")
    private String externalUrl;

    @Column(name = "in_app_screen")
    private String inAppScreen;

    @Column(name = "start_date_time")
    private LocalDateTime startDateTime;

    @Column(name = "end_date_time")
    private LocalDateTime endDateTime;

    @Column(name = "no_end_date")
    private Boolean noEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AdStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}