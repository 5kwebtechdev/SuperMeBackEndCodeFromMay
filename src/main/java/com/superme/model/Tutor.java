
package com.superme.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.superme.enums.FeeType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;




import com.fasterxml.jackson.annotation.JsonIgnore;
import com.superme.enums.FeeType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "tutors")
public class Tutor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false)
    private String name;

    @Size(max = 200)
    private String headline;

//    @NotNull
    @Min(18)
    private Integer age;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false)
    private String phone;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @NotNull
    private Gender gender;

    @NotBlank
    private String qualification;

    @Enumerated(EnumType.STRING)
    @NotNull
    private Experience experience;

    @Enumerated(EnumType.STRING)
    @NotNull
    private EntityType entityType;

    @NotBlank
    private String entityName;

    // ------------------- Collections -------------------

//    @ElementCollection(fetch = FetchType.LAZY)
//    @Enumerated(EnumType.STRING)
////    @Builder.Default
//    private List<Subject> subjects = new ArrayList<>();

    @NotBlank
    private String location;

    private String addressLine;
    private String state;
    private String city;
    private String pincode;

    private String profilePicUrl;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    private FeeType feeType;

    private BigDecimal fees;

//    @ElementCollection(fetch = FetchType.LAZY)
////    @Builder.Default
//    private List<String> levels = new ArrayList<>();

    private String documentsVerificationUrl;

//    @NotNull
    @DecimalMin("0.0")
    private BigDecimal hourlyRate;

//    @ElementCollection(fetch = FetchType.LAZY)
//    @Enumerated(EnumType.STRING)
////    @Builder.Default
//    private List<Availability> availability = new ArrayList<>();


//    @ElementCollection
//    @CollectionTable(name = "tutor_availability",
//            joinColumns = @JoinColumn(name = "tutor_id"))
//    @Column(name = "availability")
//    private List<Availability> availability;


    @ElementCollection(fetch = FetchType.LAZY)
    @Enumerated(EnumType.STRING)
    @CollectionTable(
            name = "tutor_subjects",
            joinColumns = @JoinColumn(name = "tutor_id")
    )
    @Column(name = "subject")
    @Builder.Default
    private List<Subject> subjects = new ArrayList<>();


    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "tutor_levels",
            joinColumns = @JoinColumn(name = "tutor_id")
    )
    @Column(name = "level")
    @Builder.Default
    private List<String> levels = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @Enumerated(EnumType.STRING)
    @CollectionTable(
            name = "tutor_availability",
            joinColumns = @JoinColumn(name = "tutor_id")
    )
    @Column(name = "availability")
    @Builder.Default
    private List<Availability> availability = new ArrayList<>();



    @ElementCollection(fetch = FetchType.LAZY)
    @Enumerated(EnumType.STRING)
    @CollectionTable(
            name = "tutor_contact_modes",
            joinColumns = @JoinColumn(name = "tutor_id")
    )
    @Column(name = "contact_mode")
    @Builder.Default
    private List<ContactMode> contactModes = new ArrayList<>();

//    @ElementCollection(fetch = FetchType.LAZY)
//    @Enumerated(EnumType.STRING)
////    @Builder.Default
//    private List<ContactMode> contactModes = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private Boolean isVerified = false;

    private String verificationNotes;
    private String adminNotes;

    private LocalDateTime lastLoginAt;

    @Builder.Default
    private Integer totalStudents = 0;

    private BigDecimal rating;

    @Builder.Default
    private Integer totalReviews = 0;

    // ------------------- Relations -------------------

    @OneToMany(mappedBy = "tutor", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
//    @Builder.Default
    private List<Like> likes = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "tutor_id")
//    @Builder.Default
    private List<TutorCategoryMapping> categoryMappings = new ArrayList<>();

    // ------------------- Lifecycle -------------------

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (isActive == null) isActive = true;
        if (isVerified == null) isVerified = false;

        if (subjects == null) subjects = new ArrayList<>();
        if (availability == null) availability = new ArrayList<>();
        if (contactModes == null) contactModes = new ArrayList<>();
        if (levels == null) levels = new ArrayList<>();
        if (categoryMappings == null) categoryMappings = new ArrayList<>();
        if (likes == null) likes = new ArrayList<>();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ------------------- Utility -------------------

    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }

    public boolean isVerified() {
        return Boolean.TRUE.equals(isVerified);
    }

    public int getChampsLiked() {
        return likes != null ? likes.size() : 0;
    }

    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    // ✅ ADDED FROM ORIGINAL

    @Transient
    public List<Integer> getCategoryIds() {
        if (categoryMappings == null) return List.of();

        return categoryMappings.stream()
                .map(TutorCategoryMapping::getCategoryId)
                .toList();
    }

    public boolean teachesCategory(Integer categoryId) {
        return categoryMappings != null &&
                categoryMappings.stream()
                        .anyMatch(m -> Objects.equals(m.getCategoryId(), categoryId));
    }

    public boolean hasSubject(Subject subject) {
        return subjects != null && subjects.contains(subject);
    }

    public boolean isAvailable(Availability day) {
        return availability != null && availability.contains(day);
    }

    public boolean supportsContactMode(ContactMode mode) {
        return contactModes != null && contactModes.contains(mode);
    }

    public boolean isValidForVerification() {
        return name != null && !name.isBlank()
                && email != null && !email.isBlank()
                && phone != null && !phone.isBlank()
                && qualification != null && !qualification.isBlank()
                && subjects != null && !subjects.isEmpty()
                && hourlyRate != null && hourlyRate.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean hasCompleteProfile() {
        return isValidForVerification()
                && profilePicUrl != null && !profilePicUrl.isBlank()
                && documentsVerificationUrl != null && !documentsVerificationUrl.isBlank()
                && availability != null && !availability.isEmpty()
                && contactModes != null && !contactModes.isEmpty();
    }

    // ------------------- ENUMS -------------------

    public enum Gender {
        MALE("Male"), FEMALE("Female"), OTHER("Other");
        private final String displayName;
        Gender(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    public enum Experience {
        FRESHER("Fresher"), PLUS_1("1+ Years"), PLUS_2("2+ Years"),
        PLUS_5("5+ Years"), PLUS_8("8+ Years"), PLUS_10("10+ Years");
        private final String displayName;
        Experience(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    public enum EntityType {
        SCHOOL("School"), COLLEGE("College"), TUITION("Tuition"),
        UNIVERSITY("University"), INSTITUTE("Institute"),
        SELF("Self"), INDIVIDUAL("Individual"), ACADEMY("Academy");
        private final String displayName;
        EntityType(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    public enum Subject {
        MATHEMATICS("Mathematics"), SCIENCE("Science"), ENGLISH("English"),
        PHYSICS("Physics"), CHEMISTRY("Chemistry"), BIOLOGY("Biology");
        private final String displayName;
        Subject(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    public enum Availability {
        MONDAY("Monday"), TUESDAY("Tuesday"), WEDNESDAY("Wednesday"),
        THURSDAY("Thursday"), FRIDAY("Friday"), SATURDAY("Saturday"), SUNDAY("Sunday");
        private final String displayName;
        Availability(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    public enum ContactMode {
        VIDEO_CALL("Video Call"), PHONE_CALL("Phone Call"),
        IN_PERSON("In Person"), ONLINE_CHAT("Online Chat"), HYBRID("Hybrid");
        private final String displayName;
        ContactMode(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }
}