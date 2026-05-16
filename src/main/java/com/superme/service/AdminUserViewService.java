package com.superme.service;

import com.superme.dto.*;
import com.superme.model.User;
import com.superme.model.Family;
import com.superme.dto.AdminUserViewDTO.FilterCriteria;
import com.superme.dto.AdminUserViewDTO.FilterStatistics;
import com.superme.repository.UserRepository;
import com.superme.specification.UserSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminUserViewService {

  @Autowired
  private UserRepository userRepository;

  private final UserMapper userMapper = new UserMapper();

  // ============================================================================
  // CORE USER VIEW METHODS
  // ============================================================================

  public List<AdminUserViewDTO> getAllUserViews() {
    try {
      List<User> users = userRepository.findAll();
      List<AdminUserViewDTO> result = new ArrayList<>();

      for (User user : users) {
        AdminUserViewDTO dto = createUserViewDTO(user);
        result.add(dto);
      }

      return result;
    } catch (Exception e) {
      System.err.println("Error fetching user views: " + e.getMessage());
      return new ArrayList<>();
    }
  }

  private AdminUserViewDTO createUserViewDTO(User user) {
    AdminUserViewDTO dto = new AdminUserViewDTO();

    dto.setUserId(user.getId());
    // Removed username
    dto.setName(safeTrim(user.getName()));
    dto.setGender(safeTrim(user.getGender()));
    dto.setEmail(safeTrim(user.getEmail()));
    dto.setPhone(safeTrim(user.getPhone()));
    dto.setLastLogin(user.getLastLoginDate());

    // Relationship directly from User (enum as string)
    dto.setRelationship(user.getRelationship() != null ? user.getRelationship().name().toLowerCase() : null);

    // Family info
    Family family = user.getFamily();
    if (family != null) {
      dto.setFamilyId(family.getId());
      dto.setFamilyName(safeTrim(family.getFamilyName()));
    } else {
      dto.setFamilyId(null);
      dto.setFamilyName(null);
    }

    return dto;
  }

  // ============================================================================
  // SEARCH AND FILTER METHODS
  // ============================================================================

  public List<AdminUserViewDTO> searchUsers(String searchTerm) {
    try {
      List<AdminUserViewDTO> allUsers = getAllUserViews();
      return AdminUserViewDTO.filterBySearchTerm(allUsers, searchTerm);
    } catch (Exception e) {
      System.err.println("Error searching users: " + e.getMessage());
      return List.of();
    }
  }

  public List<AdminUserViewDTO> filterByGender(List<String> genders) {
    try {
      List<AdminUserViewDTO> allUsers = getAllUserViews();
      return AdminUserViewDTO.filterByGender(allUsers, genders);
    } catch (Exception e) {
      System.err.println("Error filtering users by gender: " + e.getMessage());
      return List.of();
    }
  }

  public List<AdminUserViewDTO> filterByRelationship(List<String> relationships) {
    try {
      List<AdminUserViewDTO> allUsers = getAllUserViews();
      return AdminUserViewDTO.filterByRelationship(allUsers, relationships);
    } catch (Exception e) {
      System.err.println("Error filtering users by relationship: " + e.getMessage());
      return List.of();
    }
  }

  public List<AdminUserViewDTO> filterByActivityStatus(Boolean activeOnly, Boolean inactiveOnly) {
    try {
      List<AdminUserViewDTO> allUsers = getAllUserViews();
      return AdminUserViewDTO.filterByActivityStatus(allUsers, activeOnly, inactiveOnly);
    } catch (Exception e) {
      System.err.println("Error filtering users by activity status: " + e.getMessage());
      return List.of();
    }
  }

  public List<AdminUserViewDTO> filterUsers(FilterCriteria criteria) {
    try {
      List<AdminUserViewDTO> allUsers = getAllUserViews();
      return AdminUserViewDTO.filterByAllCriteria(allUsers, criteria);
    } catch (Exception e) {
      System.err.println("Error filtering users with criteria: " + e.getMessage());
      return List.of();
    }
  }

  public List<AdminUserViewDTO> filterUsersWithPagination(FilterCriteria criteria, int limit, int offset) {
    try {
      List<AdminUserViewDTO> filteredUsers = filterUsers(criteria);

      int start = Math.min(offset, filteredUsers.size());
      int end = Math.min(start + limit, filteredUsers.size());

      return filteredUsers.subList(start, end);
    } catch (Exception e) {
      System.err.println("Error filtering users with pagination: " + e.getMessage());
      return List.of();
    }
  }

  public List<AdminUserViewDTO> searchUsersWithPagination(String searchTerm, int limit, int offset) {
    FilterCriteria criteria = new FilterCriteria();
    criteria.setSearchTerm(searchTerm);
    return filterUsersWithPagination(criteria, limit, offset);
  }

  public long getFilterResultsCount(FilterCriteria criteria) {
    try {
      return filterUsers(criteria).size();
    } catch (Exception e) {
      System.err.println("Error counting filter results: " + e.getMessage());
      return 0L;
    }
  }

  public long getSearchResultsCount(String searchTerm) {
    FilterCriteria criteria = new FilterCriteria();
    criteria.setSearchTerm(searchTerm);
    return getFilterResultsCount(criteria);
  }

  public FilterStatistics getFilterStatistics() {
    try {
      List<AdminUserViewDTO> allUsers = getAllUserViews();
      return AdminUserViewDTO.getFilterStatistics(allUsers);
    } catch (Exception e) {
      System.err.println("Error calculating filter statistics: " + e.getMessage());
      return new FilterStatistics();
    }
  }

  public FilterStatistics getFilterStatistics(FilterCriteria criteria) {
    try {
      List<AdminUserViewDTO> filteredUsers = filterUsers(criteria);
      return AdminUserViewDTO.getFilterStatistics(filteredUsers);
    } catch (Exception e) {
      System.err.println("Error calculating filtered statistics: " + e.getMessage());
      return new FilterStatistics();
    }
  }

  // ============================================================================
  // SPECIALIZED FILTER METHODS
  // ============================================================================

  public List<AdminUserViewDTO> getUsersByGender(String gender) {
    return filterByGender(Arrays.asList(gender));
  }

  public List<AdminUserViewDTO> getUsersByRelationship(String relationship) {
    return filterByRelationship(Arrays.asList(relationship));
  }

  public List<AdminUserViewDTO> getActiveUsers() {
    return filterByActivityStatus(true, false);
  }

  public List<AdminUserViewDTO> getInactiveUsers() {
    return filterByActivityStatus(false, true);
  }

  public List<AdminUserViewDTO> getUsersWithValidGender() {
    try {
      List<AdminUserViewDTO> allUsers = getAllUserViews();
      return allUsers.stream()
          .filter(AdminUserViewDTO::hasValidGender)
          .collect(Collectors.toList());
    } catch (Exception e) {
      System.err.println("Error filtering users with valid gender: " + e.getMessage());
      return List.of();
    }
  }

  public List<AdminUserViewDTO> getUsersWithValidRelationship() {
    try {
      List<AdminUserViewDTO> allUsers = getAllUserViews();
      return allUsers.stream()
          .filter(AdminUserViewDTO::hasValidRelationship)
          .collect(Collectors.toList());
    } catch (Exception e) {
      System.err.println("Error filtering users with valid relationship: " + e.getMessage());
      return List.of();
    }
  }

  public List<AdminUserViewDTO> getUsersWithoutFamily() {
    try {
      List<AdminUserViewDTO> allUsers = getAllUserViews();
      return allUsers.stream()
          .filter(user -> user.getFamilyId() == null)
          .collect(Collectors.toList());
    } catch (Exception e) {
      System.err.println("Error filtering users without family: " + e.getMessage());
      return List.of();
    }
  }

  public List<AdminUserViewDTO> getUsersWithFamily() {
    try {
      List<AdminUserViewDTO> allUsers = getAllUserViews();
      return allUsers.stream()
          .filter(user -> user.getFamilyId() != null)
          .collect(Collectors.toList());
    } catch (Exception e) {
      System.err.println("Error filtering users with family: " + e.getMessage());
      return List.of();
    }
  }

  // ============================================================================
  // STATISTICS AND ANALYTICS
  // ============================================================================

  public UserStatisticsDto getUserStatistics() {
    try {
      LocalDateTime now = LocalDateTime.now();
      LocalDateTime thirtyDaysAgo = now.minusDays(30);

      LocalDateTime startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth()).withHour(0).withMinute(0).withSecond(0)
          .withNano(0);
      LocalDateTime startOfToday = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
      LocalDateTime startOfWeek = now.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).withHour(0)
          .withMinute(0).withSecond(0).withNano(0);

      Long totalUsers = safeCount(() -> userRepository.count());
      Long activeUsers = safeCount(() -> countActiveUsers(thirtyDaysAgo));
      Long inactiveUsers = Math.max(0L, totalUsers - activeUsers);
      Long monthlyRegistrations = safeCount(() -> countNewRegistrations(startOfMonth));
      Long todayRegistrations = safeCount(() -> countNewRegistrations(startOfToday));
      Long weeklyRegistrations = safeCount(() -> countNewRegistrations(startOfWeek));

      return new UserStatisticsDto(
          totalUsers,
          activeUsers,
          inactiveUsers,
          monthlyRegistrations,
          todayRegistrations,
          weeklyRegistrations,null);

    } catch (Exception e) {
      System.err.println("Error calculating user statistics: " + e.getMessage());
      return new UserStatisticsDto();
    }
  }

  public Map<String, Object> getDetailedUserAnalytics() {
    try {
      Map<String, Object> analytics = new HashMap<>();

      UserStatisticsDto stats = getUserStatistics();
      List<AdminUserViewDTO> users = getAllUserViews();
      FilterStatistics filterStats = getFilterStatistics();

      analytics.put("userStatistics", stats);
      analytics.put("filterStatistics", filterStats);
      analytics.put("totalUserCount", users.size());
      analytics.put("usersWithFamilies", countUsersWithFamilies(users));
      analytics.put("usersWithoutFamilies", users.size() - countUsersWithFamilies(users));
      analytics.put("genderDistribution", getGenderDistribution(users));
      analytics.put("relationshipDistribution", getRelationshipDistribution(users));
      analytics.put("activityDistribution", getActivityDistribution(users));

      return analytics;
    } catch (Exception e) {
      System.err.println("Error calculating detailed analytics: " + e.getMessage());
      return new HashMap<>();
    }
  }

  public Map<String, Object> getDetailedAnalyticsWithFilters(FilterCriteria criteria) {
    try {
      Map<String, Object> analytics = new HashMap<>();

      List<AdminUserViewDTO> allUsers = getAllUserViews();
      List<AdminUserViewDTO> filteredUsers = criteria != null ? AdminUserViewDTO.filterByAllCriteria(allUsers, criteria)
          : allUsers;

      UserStatisticsDto globalStats = getUserStatistics();
      FilterStatistics allUsersStats = AdminUserViewDTO.getFilterStatistics(allUsers);
      FilterStatistics filteredStats = AdminUserViewDTO.getFilterStatistics(filteredUsers);

      analytics.put("globalStatistics", globalStats);
      analytics.put("allUsersFilterStats", allUsersStats);
      analytics.put("filteredStats", filteredStats);
      analytics.put("filterCriteria", criteria);
      analytics.put("totalUsers", allUsers.size());
      analytics.put("filteredUsers", filteredUsers.size());
      analytics.put("filterEfficiency", calculateFilterEfficiency(allUsers.size(), filteredUsers.size()));

      analytics.put("genderBreakdown", getGenderBreakdown(filteredUsers));
      analytics.put("relationshipBreakdown", getRelationshipBreakdown(filteredUsers));
      analytics.put("activityBreakdown", getActivityBreakdown(filteredUsers));
      analytics.put("familyBreakdown", getFamilyBreakdown(filteredUsers));

      return analytics;
    } catch (Exception e) {
      System.err.println("Error calculating detailed analytics with filters: " + e.getMessage());
      return new HashMap<>();
    }
  }

  // ============================================================================
  // HELPER METHODS
  // ============================================================================

  private double calculateFilterEfficiency(int total, int filtered) {
    if (total == 0)
      return 0.0;
    return ((double) filtered / total) * 100.0;
  }

  private Map<String, Object> getGenderBreakdown(List<AdminUserViewDTO> users) {
    Map<String, Object> breakdown = new HashMap<>();
    Map<String, Long> distribution = getGenderDistribution(users);

    breakdown.put("distribution", distribution);
    breakdown.put("maleCount", distribution.getOrDefault("male", 0L));
    breakdown.put("femaleCount", distribution.getOrDefault("female", 0L));
    breakdown.put("unspecifiedCount", distribution.getOrDefault("Not Specified", 0L));
    breakdown.put("malePercentage", calculatePercentage(distribution.getOrDefault("male", 0L), users.size()));
    breakdown.put("femalePercentage", calculatePercentage(distribution.getOrDefault("female", 0L), users.size()));

    return breakdown;
  }

  private Map<String, Object> getRelationshipBreakdown(List<AdminUserViewDTO> users) {
    Map<String, Object> breakdown = new HashMap<>();
    Map<String, Long> distribution = getRelationshipDistribution(users);

    breakdown.put("distribution", distribution);
    breakdown.put("selfCount", distribution.getOrDefault("self", 0L));
    breakdown.put("parentCount", distribution.getOrDefault("parent", 0L));
    breakdown.put("childCount", distribution.getOrDefault("child", 0L));
    breakdown.put("unspecifiedCount", distribution.getOrDefault("Not Specified", 0L));

    return breakdown;
  }

  private Map<String, Object> getActivityBreakdown(List<AdminUserViewDTO> users) {
    Map<String, Object> breakdown = new HashMap<>();

    long activeCount = users.stream().filter(AdminUserViewDTO::isActiveUser).count();
    long inactiveCount = users.size() - activeCount;

    breakdown.put("activeCount", activeCount);
    breakdown.put("inactiveCount", inactiveCount);
    breakdown.put("activePercentage", calculatePercentage(activeCount, users.size()));
    breakdown.put("inactivePercentage", calculatePercentage(inactiveCount, users.size()));

    return breakdown;
  }

  private Map<String, Object> getFamilyBreakdown(List<AdminUserViewDTO> users) {
    Map<String, Object> breakdown = new HashMap<>();

    long withFamilyCount = users.stream().filter(user -> user.getFamilyId() != null).count();
    long withoutFamilyCount = users.size() - withFamilyCount;

    breakdown.put("withFamilyCount", withFamilyCount);
    breakdown.put("withoutFamilyCount", withoutFamilyCount);
    breakdown.put("withFamilyPercentage", calculatePercentage(withFamilyCount, users.size()));
    breakdown.put("withoutFamilyPercentage", calculatePercentage(withoutFamilyCount, users.size()));

    return breakdown;
  }

  private double calculatePercentage(long count, int total) {
    if (total == 0)
      return 0.0;
    return ((double) count / total) * 100.0;
  }

  private Long countActiveUsers(LocalDateTime since) {
    try {
      return userRepository.findAll().stream()
          .filter(user -> user.getLastLoginDate() != null && user.getLastLoginDate().isAfter(since))
          .count();
    } catch (Exception e) {
      System.err.println("Error counting active users: " + e.getMessage());
      return 0L;
    }
  }

  private Long countNewRegistrations(LocalDateTime since) {
    try {
      return userRepository.findAll().stream()
          .filter(user -> user.getCreatedDateTime() != null && user.getCreatedDateTime().isAfter(since))
          .count();
    } catch (Exception e) {
      System.err.println("Error counting new registrations: " + e.getMessage());
      return 0L;
    }
  }

  private long countUsersWithFamilies(List<AdminUserViewDTO> users) {
    return users.stream()
        .filter(user -> user.getFamilyId() != null)
        .count();
  }

  private Map<String, Long> getGenderDistribution(List<AdminUserViewDTO> users) {
    Map<String, Long> distribution = new HashMap<>();

    users.forEach(user -> {
      String gender = user.getGender();
      if (gender == null || "N/A".equals(gender)) {
        gender = "Not Specified";
      }
      distribution.merge(gender, 1L, Long::sum);
    });

    return distribution;
  }

  private Map<String, Long> getRelationshipDistribution(List<AdminUserViewDTO> users) {
    Map<String, Long> distribution = new HashMap<>();

    users.forEach(user -> {
      String relationship = user.getRelationship();
      if (relationship == null || "N/A".equals(relationship)) {
        relationship = "Not Specified";
      }
      distribution.merge(relationship, 1L, Long::sum);
    });

    return distribution;
  }

  private Map<String, Long> getActivityDistribution(List<AdminUserViewDTO> users) {
    Map<String, Long> distribution = new HashMap<>();

    users.forEach(user -> {
      String status = user.isActiveUser() ? "Active" : "Inactive";
      distribution.merge(status, 1L, Long::sum);
    });

    return distribution;
  }

  public List<AdminUserViewDTO> getRecentlyActiveUsers(int limit) {
    try {
      List<AdminUserViewDTO> allUsers = getAllUserViews();

      return allUsers.stream()
          .filter(user -> user.getLastLogin() != null)
          .sorted((u1, u2) -> u2.getLastLogin().compareTo(u1.getLastLogin()))
          .limit(limit)
          .collect(Collectors.toList());
    } catch (Exception e) {
      System.err.println("Error fetching recently active users: " + e.getMessage());
      return new ArrayList<>();
    }
  }

  public List<AdminUserViewDTO> getUsersNeverLoggedIn() {
    try {
      List<AdminUserViewDTO> allUsers = getAllUserViews();

      return allUsers.stream()
          .filter(user -> user.getLastLogin() == null)
          .collect(Collectors.toList());
    } catch (Exception e) {
      System.err.println("Error fetching users who never logged in: " + e.getMessage());
      return new ArrayList<>();
    }
  }

  private String safeTrim(String value) {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    return value.trim();
  }

  private Long safeCount(CountOperation operation) {
    try {
      return operation.count();
    } catch (Exception e) {
      System.err.println("Error in count operation: " + e.getMessage());
      return 0L;
    }
  }

  @FunctionalInterface
  private interface CountOperation {
    Long count();
  }

  // ============================================================================
  // FILTER OPTIONS METHODS
  // ============================================================================

  public Map<String, Object> getAvailableFilterOptions() {
    Map<String, Object> options = new HashMap<>();

    options.put("genders", AdminUserViewDTO.Gender.getAllValues());
    options.put("relationships", AdminUserViewDTO.Relationship.getAllValues());
    options.put("activityStatuses", Arrays.asList("active", "inactive"));

    List<AdminUserViewDTO> allUsers = getAllUserViews();
    options.put("availableGenders", getAvailableGenders(allUsers));
    options.put("availableRelationships", getAvailableRelationships(allUsers));
    options.put("statisticsSummary", getFilterStatistics());

    return options;
  }

  private List<String> getAvailableGenders(List<AdminUserViewDTO> users) {
    return users.stream()
        .map(AdminUserViewDTO::getGender)
        .filter(gender -> !"N/A".equals(gender))
        .distinct()
        .sorted()
        .collect(Collectors.toList());
  }

  private List<String> getAvailableRelationships(List<AdminUserViewDTO> users) {
    return users.stream()
        .map(AdminUserViewDTO::getRelationship)
        .filter(relationship -> !"N/A".equals(relationship))
        .distinct()
        .sorted()
        .collect(Collectors.toList());
  }


  public byte[] generateUsersExcel(String q) throws IOException {
    List<User> users = userRepository.findAll();

    if (q != null && !q.trim().isEmpty()) {
      String term = q.toLowerCase().trim();
      users = users.stream()
          .filter(u -> matchesExcelQuery(u, term))
          .collect(Collectors.toList());
    }

    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Users");

      Row header = sheet.createRow(0);
      String[] cols = {"User ID", "Name", "Email", "Role", "Relationship", "Status", "Last Active"};
      for (int i = 0; i < cols.length; i++) {
        header.createCell(i).setCellValue(cols[i]);
      }

      int rowIdx = 1;
      for (User user : users) {
        Row row = sheet.createRow(rowIdx++);
        row.createCell(0).setCellValue(user.getId() != null ? (double) user.getId() : 0);
        row.createCell(1).setCellValue(user.getName() != null ? user.getName() : "");
        row.createCell(2).setCellValue(user.getEmail() != null ? user.getEmail() : "");
        row.createCell(3).setCellValue(user.getRole() != null ? user.getRole().name() : "");
        row.createCell(4).setCellValue(user.getRelationship() != null ? user.getRelationship().name().toLowerCase() : "");
        row.createCell(5).setCellValue(user.isEnabled() ? "active" : "inactive");
        row.createCell(6).setCellValue(user.getLastLoginDate() != null ? user.getLastLoginDate().toString() : "");
      }

      ByteArrayOutputStream out = new ByteArrayOutputStream();
      workbook.write(out);
      return out.toByteArray();
    }
  }

  private boolean matchesExcelQuery(User user, String term) {
    return (user.getName() != null && user.getName().toLowerCase().contains(term))
        || (user.getEmail() != null && user.getEmail().toLowerCase().contains(term))
        || (user.getId() != null && user.getId().toString().contains(term))
        || (user.getRelationship() != null && user.getRelationship().name().toLowerCase().contains(term));
  }

  public AdminSearchResponseDto searchUsersAdmin(UserSearchRequestDto req,
                                                 int page,
                                                 int size,
                                                 String sortBy,
                                                 String direction) {

    // Sorting
    Sort sort = direction.equalsIgnoreCase("DESC")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();

    Pageable pageable = PageRequest.of(page, size, sort);

    // Specification
    Specification<User> spec = UserSpecification.fromRequest(req);

    // Fetch paginated result
    Page<User> users = userRepository.findAll(spec, pageable);

    // Map to DTOs
    List<UserResponseAdminDto> userResponseList =
            users.stream().map(userMapper::toAdminDto).toList();

    UserStatisticsDto statistics = getUserStatistics();

    // ✅ Build final paginated response
    return AdminSearchResponseDto.builder()
            .users(userResponseList)
            .statistics(statistics)
            .page(users.getNumber())         // current page index
            .size(users.getSize())           // size per page
            .totalElements(users.getTotalElements())
            .totalPages(users.getTotalPages())
            .build();
  }


  @Component
  public static class UserMapper {

    public UserResponseAdminDto toAdminDto(User user) {
      if (user == null) {
        return null;
      }

      return UserResponseAdminDto.builder()
              .id(user.getId())
              .name(user.getName())
              .email(user.getEmail())
              .phone(user.getPhone())
              .role(user.getRole())
              .gender(user.getGender())
              .relationship(user.getRelationship())
              .age(user.getAge())
              .ageGroup(user.getAgeGroup())
              .coins(user.getCoins())
              .currentStreak(user.getCurrentStreak())
              .highestStreak(user.getHighestStreak())
              .lastLoginDate(user.getLastLoginDate())
              .createdDateTime(user.getCreatedDateTime())
              .enabled(user.isEnabled())
              .isLoggedIn(user.isLoggedIn())
              .emailVerified(user.isEmailVerified())
              .familyId(user.getFamily() != null ? user.getFamily().getId() : null)
              .avatarId(user.getAvatar() != null ? user.getAvatar().getId() : null)
              .petId(user.getPet() != null ? user.getPet().getId() : null)
              .build();
    }
  }
}
