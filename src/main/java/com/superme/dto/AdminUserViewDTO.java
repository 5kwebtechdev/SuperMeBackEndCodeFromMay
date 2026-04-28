package com.superme.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Arrays;

@Data
public class AdminUserViewDTO {
  private Long userId;
  // Removed username field
  private String name;
  private String gender;
  private String email;
  private String phone;
  private LocalDateTime lastLogin;
  private String relationship;
  private Long familyId;
  private String familyName;

  // Statistics fields for stat cards
  private UserStatisticsDto statistics;

  // ============================================================================
  // ENUMS FOR FILTER OPTIONS
  // ============================================================================

  public enum Gender {
    MALE("male"),
    FEMALE("female");

    private final String value;

    Gender(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    public static Gender fromString(String value) {
      if (value == null)
        return null;
      for (Gender gender : Gender.values()) {
        if (gender.value.equalsIgnoreCase(value.trim())) {
          return gender;
        }
      }
      return null;
    }

    public static List<String> getAllValues() {
      return Arrays.stream(Gender.values())
          .map(Gender::getValue)
          .collect(Collectors.toList());
    }
  }

  public enum Relationship {
    SELF("self"),
    PARENT("parent"),
    CHILD("child");

    private final String value;

    Relationship(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }



    public static List<String> getAllValues() {
      return Arrays.stream(Relationship.values())
          .map(Relationship::getValue)
          .collect(Collectors.toList());
    }
  }

  // ============================================================================
  // FILTER CRITERIA CLASS
  // ============================================================================

  public static class FilterCriteria {
    private String searchTerm;
    private List<String> genders;
    private List<String> relationships;
    private Boolean activeOnly;
    private Boolean inactiveOnly;

    public FilterCriteria() {
    }

    public FilterCriteria(String searchTerm, List<String> genders, List<String> relationships) {
      this.searchTerm = searchTerm;
      this.genders = genders;
      this.relationships = relationships;
    }

    // Getters and setters
    public String getSearchTerm() {
      return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
      this.searchTerm = searchTerm;
    }

    public List<String> getGenders() {
      return genders;
    }

    public void setGenders(List<String> genders) {
      this.genders = genders;
    }

    public List<String> getRelationships() {
      return relationships;
    }

    public void setRelationships(List<String> relationships) {
      this.relationships = relationships;
    }

    public Boolean getActiveOnly() {
      return activeOnly;
    }

    public void setActiveOnly(Boolean activeOnly) {
      this.activeOnly = activeOnly;
    }

    public Boolean getInactiveOnly() {
      return inactiveOnly;
    }

    public void setInactiveOnly(Boolean inactiveOnly) {
      this.inactiveOnly = inactiveOnly;
    }

    public boolean hasFilters() {
      return (searchTerm != null && !searchTerm.trim().isEmpty()) ||
          (genders != null && !genders.isEmpty()) ||
          (relationships != null && !relationships.isEmpty()) ||
          Boolean.TRUE.equals(activeOnly) ||
          Boolean.TRUE.equals(inactiveOnly);
    }

    public String getSummary() {
      StringBuilder summary = new StringBuilder();

      if (searchTerm != null && !searchTerm.trim().isEmpty()) {
        summary.append("Search: '").append(searchTerm).append("' ");
      }

      if (genders != null && !genders.isEmpty()) {
        summary.append("Gender: ").append(String.join(", ", genders)).append(" ");
      }

      if (relationships != null && !relationships.isEmpty()) {
        summary.append("Relationship: ").append(String.join(", ", relationships)).append(" ");
      }

      if (Boolean.TRUE.equals(activeOnly)) {
        summary.append("Active users only ");
      }

      if (Boolean.TRUE.equals(inactiveOnly)) {
        summary.append("Inactive users only ");
      }

      return summary.toString().trim();
    }
  }

  // ============================================================================
  // CONSTRUCTORS
  // ============================================================================

  public AdminUserViewDTO() {
    this.statistics = new UserStatisticsDto();
  }


  // ============================================================================
  // SEARCH FUNCTIONALITY
  // ============================================================================

  public boolean matchesSearchTerm(String searchTerm) {
    if (searchTerm == null || searchTerm.trim().isEmpty()) {
      return true;
    }

    String normalizedSearchTerm = searchTerm.toLowerCase().trim();

    return (getName() != null && !"N/A".equals(getName()) &&
        getName().toLowerCase().contains(normalizedSearchTerm)) ||
        (getEmail() != null && !"N/A".equals(getEmail()) &&
            getEmail().toLowerCase().contains(normalizedSearchTerm))
        ||
        (getPhone() != null && !"N/A".equals(getPhone()) &&
            getPhone().toLowerCase().contains(normalizedSearchTerm))
        ||
        (getUserId() != null &&
            getUserId().toString().contains(normalizedSearchTerm))
        ||
        (getFamilyName() != null && !"N/A".equals(getFamilyName()) &&
            getFamilyName().toLowerCase().contains(normalizedSearchTerm));
  }

  // ============================================================================
  // FILTER FUNCTIONALITY
  // ============================================================================

  public boolean matchesGenderFilter(List<String> genderFilters) {
    if (genderFilters == null || genderFilters.isEmpty()) {
      return true;
    }
    String userGender = getGender();
    if ("N/A".equals(userGender)) {
      return false;
    }
    return genderFilters.stream()
        .anyMatch(filter -> filter.equalsIgnoreCase(userGender));
  }

  public boolean matchesRelationshipFilter(List<String> relationshipFilters) {
    if (relationshipFilters == null || relationshipFilters.isEmpty()) {
      return true;
    }
    String userRelationship = getRelationship();
    if ("N/A".equals(userRelationship)) {
      return false;
    }
    return relationshipFilters.stream()
        .anyMatch(filter -> filter.equalsIgnoreCase(userRelationship));
  }

  public boolean matchesActivityFilter(Boolean activeOnly, Boolean inactiveOnly) {
    boolean userIsActive = isActiveUser();

    if (Boolean.TRUE.equals(activeOnly) && Boolean.TRUE.equals(inactiveOnly)) {
      return true;
    }

    if (Boolean.TRUE.equals(activeOnly)) {
      return userIsActive;
    }

    if (Boolean.TRUE.equals(inactiveOnly)) {
      return !userIsActive;
    }

    return true;
  }

  public boolean matchesFilterCriteria(FilterCriteria criteria) {
    if (criteria == null) {
      return true;
    }

    return matchesSearchTerm(criteria.getSearchTerm()) &&
        matchesGenderFilter(criteria.getGenders()) &&
        matchesRelationshipFilter(criteria.getRelationships()) &&
        matchesActivityFilter(criteria.getActiveOnly(), criteria.getInactiveOnly());
  }

  // ============================================================================
  // STATIC FILTER METHODS
  // ============================================================================

  public static List<AdminUserViewDTO> filterBySearchTerm(List<AdminUserViewDTO> users, String searchTerm) {
    if (users == null) {
      return List.of();
    }
    return users.stream()
        .filter(user -> user.matchesSearchTerm(searchTerm))
        .collect(Collectors.toList());
  }

  public static List<AdminUserViewDTO> filterByGender(List<AdminUserViewDTO> users, List<String> genders) {
    if (users == null) {
      return List.of();
    }
    return users.stream()
        .filter(user -> user.matchesGenderFilter(genders))
        .collect(Collectors.toList());
  }

  public static List<AdminUserViewDTO> filterByRelationship(List<AdminUserViewDTO> users, List<String> relationships) {
    if (users == null) {
      return List.of();
    }
    return users.stream()
        .filter(user -> user.matchesRelationshipFilter(relationships))
        .collect(Collectors.toList());
  }

  public static List<AdminUserViewDTO> filterByActivityStatus(List<AdminUserViewDTO> users, Boolean activeOnly,
      Boolean inactiveOnly) {
    if (users == null) {
      return List.of();
    }
    return users.stream()
        .filter(user -> user.matchesActivityFilter(activeOnly, inactiveOnly))
        .collect(Collectors.toList());
  }

  public static List<AdminUserViewDTO> filterByAllCriteria(List<AdminUserViewDTO> users, FilterCriteria criteria) {
    if (users == null) {
      return List.of();
    }
    if (criteria == null || !criteria.hasFilters()) {
      return users;
    }
    return users.stream()
        .filter(user -> user.matchesFilterCriteria(criteria))
        .collect(Collectors.toList());
  }

  public static FilterStatistics getFilterStatistics(List<AdminUserViewDTO> users) {
    if (users == null || users.isEmpty()) {
      return new FilterStatistics();
    }
    return new FilterStatistics(users);
  }

  // ============================================================================
  // FILTER STATISTICS CLASS
  // ============================================================================

  public static class FilterStatistics {
    private int totalUsers;
    private int maleUsers;
    private int femaleUsers;
    private int selfUsers;
    private int parentUsers;
    private int childUsers;
    private int activeUsers;
    private int inactiveUsers;

    public FilterStatistics() {
    }

    public FilterStatistics(List<AdminUserViewDTO> users) {
      this.totalUsers = users.size();

      for (AdminUserViewDTO user : users) {
        String gender = user.getGender();
        if ("male".equalsIgnoreCase(gender)) {
          maleUsers++;
        } else if ("female".equalsIgnoreCase(gender)) {
          femaleUsers++;
        }

        String relationship = user.getRelationship();
        if ("self".equalsIgnoreCase(relationship)) {
          selfUsers++;
        } else if ("parent".equalsIgnoreCase(relationship)) {
          parentUsers++;
        } else if ("child".equalsIgnoreCase(relationship)) {
          childUsers++;
        }

        if (user.isActiveUser()) {
          activeUsers++;
        } else {
          inactiveUsers++;
        }
      }
    }

    public int getTotalUsers() {
      return totalUsers;
    }

    public int getMaleUsers() {
      return maleUsers;
    }

    public int getFemaleUsers() {
      return femaleUsers;
    }

    public int getSelfUsers() {
      return selfUsers;
    }

    public int getParentUsers() {
      return parentUsers;
    }

    public int getChildUsers() {
      return childUsers;
    }

    public int getActiveUsers() {
      return activeUsers;
    }

    public int getInactiveUsers() {
      return inactiveUsers;
    }

    public double getMalePercentage() {
      return totalUsers > 0 ? (double) maleUsers / totalUsers * 100 : 0;
    }

    public double getFemalePercentage() {
      return totalUsers > 0 ? (double) femaleUsers / totalUsers * 100 : 0;
    }

    public double getActivePercentage() {
      return totalUsers > 0 ? (double) activeUsers / totalUsers * 100 : 0;
    }

    public String getSummary() {
      return String.format("Total: %d, Male: %d (%.1f%%), Female: %d (%.1f%%), " +
          "Self: %d, Parent: %d, Child: %d, Active: %d (%.1f%%)",
          totalUsers, maleUsers, getMalePercentage(), femaleUsers, getFemalePercentage(),
          selfUsers, parentUsers, childUsers, activeUsers, getActivePercentage());
    }
  }


  public AdminUserViewDTO getHighlightedVersion(String searchTerm) {
    if (searchTerm == null || searchTerm.trim().isEmpty()) {
      return this;
    }

    AdminUserViewDTO highlighted = new AdminUserViewDTO();
    highlighted.setUserId(this.getUserId());
    highlighted.setName(highlightText(this.getName(), searchTerm));
    highlighted.setGender(this.getGender());
    highlighted.setEmail(highlightText(this.getEmail(), searchTerm));
    highlighted.setPhone(highlightText(this.getPhone(), searchTerm));
    highlighted.setLastLogin(this.getLastLogin());
    highlighted.setRelationship(this.getRelationship());
    highlighted.setFamilyId(this.getFamilyId());
    highlighted.setFamilyName(highlightText(this.getFamilyName(), searchTerm));
    highlighted.setStatistics(this.getStatistics());

    return highlighted;
  }

  private String highlightText(String text, String searchTerm) {
    if (text == null || "N/A".equals(text) || searchTerm == null || searchTerm.trim().isEmpty()) {
      return text;
    }

    String normalizedSearch = searchTerm.trim();
    return text.replaceAll("(?i)" + normalizedSearch, "<mark>$0</mark>");
  }


  public String getFormattedLastLogin() {
    if (lastLogin == null) {
      return "Never";
    }
    return lastLogin.toString();
  }

  public boolean isActiveUser() {
    if (lastLogin == null)
      return false;
    return lastLogin.isAfter(LocalDateTime.now().minusDays(30));
  }

  public String getDisplayName() {
    String n = getName();
    return "N/A".equals(n) ? "Unknown User" : n;
  }

  public boolean hasValidGender() {
    return !"N/A".equals(getGender());
  }

  public boolean hasValidRelationship() {
    return !"N/A".equals(getRelationship());
  }





  @Override
  public String toString() {
    return "AdminUserViewDTO{" +
        "userId=" + userId +
        ", name='" + getName() + '\'' +
        ", gender='" + getGender() + '\'' +
        ", relationship='" + getRelationship() + '\'' +
        ", email='" + getEmail() + '\'' +
        ", lastLogin=" + lastLogin +
        ", familyId=" + familyId +
        ", statistics=" + statistics +
        '}';
  }
}
