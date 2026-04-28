package com.superme.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Admin Tutor management with comprehensive fields for admin dashboard.
 */
public class AdminTutorDTO {

    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;

    private String name;
    private String headline;
    private Integer age;
    private String gender;
    private String experience;
    private String qualification;
    private String phone;
    private String email;

    private Boolean documentsVerified;
    private Double hourlyRate;

    // High-level location label
    private String location;

    // NEW – granular address section
    private String addressLine;
    private String state;
    private String city;
    private String pincode;

    private List<String> subjects;
    private String entityType;
    private String entityName;

    private Integer studentCount;
    private Integer totalStudents;
    private String status;
    private String verificationStatus;
    private Integer champsLiked;
    private BigDecimal rating;
    private Integer totalReviews;

    public AdminTutorDTO() {
    }

    // Getters / setters for all fields

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getQualification() {
        return qualification;
    }

    public void setQualification(String qualification) {
        this.qualification = qualification;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getDocumentsVerified() {
        return documentsVerified;
    }

    public void setDocumentsVerified(Boolean documentsVerified) {
        this.documentsVerified = documentsVerified;
    }

    public Double getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(Double hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    // NEW address getters/setters

    public String getAddressLine() {
        return addressLine;
    }

    public void setAddressLine(String addressLine) {
        this.addressLine = addressLine;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public List<String> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<String> subjects) {
        this.subjects = subjects;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public Integer getStudentCount() {
        return studentCount;
    }

    public void setStudentCount(Integer studentCount) {
        this.studentCount = studentCount;
    }

    public Integer getTotalStudents() {
        return totalStudents != null ? totalStudents : studentCount;
    }

    public void setTotalStudents(Integer totalStudents) {
        this.totalStudents = totalStudents;
        this.studentCount = totalStudents;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public Integer getChampsLiked() {
        return champsLiked;
    }

    public void setChampsLiked(Integer champsLiked) {
        this.champsLiked = champsLiked;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public Integer getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(Integer totalReviews) {
        this.totalReviews = totalReviews;
    }

    // Nested statistics class
    public static class TutorStatistics {
        private long totalTutors;
        private long activeTutors;
        private long verifiedTutors;
        private long unverifiedTutors;

        public TutorStatistics() {
        }

        public long getTotalTutors() {
            return totalTutors;
        }

        public void setTotalTutors(long totalTutors) {
            this.totalTutors = totalTutors;
        }

        public long getActiveTutors() {
            return activeTutors;
        }

        public void setActiveTutors(long activeTutors) {
            this.activeTutors = activeTutors;
        }

        public long getVerifiedTutors() {
            return verifiedTutors;
        }

        public void setVerifiedTutors(long verifiedTutors) {
            this.verifiedTutors = verifiedTutors;
        }

        public long getUnverifiedTutors() {
            return unverifiedTutors;
        }

        public void setUnverifiedTutors(long unverifiedTutors) {
            this.unverifiedTutors = unverifiedTutors;
        }
    }
}
