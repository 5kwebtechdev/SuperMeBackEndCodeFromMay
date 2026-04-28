package com.superme.repository;

import com.superme.dto.UserStreakDTO;
import com.superme.enums.AgeGroup;
import com.superme.enums.Relationship;
import com.superme.enums.Role;
import com.superme.model.Family;
import com.superme.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    // Find a user by email
    Optional<User> findByEmail(String email);

    // Find a user by phone
    Optional<User> findByPhone(String phone);

    // Retrieve the top 10 users based on highest streak in descending order (ONLY name and streak in DTO)
//    @Query("SELECT new com.superme.dto.UserStreakDTO(u.name, u.highestStreak) FROM User u ORDER BY u.highestStreak DESC")
//    List<UserStreakDTO> findTop10ByOrderByHighestStreakDesc();

    // Retrieve all users with a specific role
    List<User> findByRole(Role role);

    // Retrieve all users who logged in at a specific date/time
    List<User> findByLastLoginDate(java.time.LocalDateTime lastLoginDate);

    // Find a user by email verification token
    Optional<User> findByEmailVerificationToken(String token);

    /**
     * Find the top 10 users with the "USER" role, ordered by coins in descending
     * order.
     */
    List<User> findTop10ByRoleOrderByCoinsDesc(Role role);

    // Find all users by family id
    List<User> findByFamilyId(Long familyId);

    // Find all users by family code (via join)
    List<User> findByFamily_FamilyCode(String familyCode);

    // Find a user by name
    Optional<User> findByName(String name);

    // ===== NEW AGE-RELATED METHODS =====

    // Find users by specific age
    List<User> findByAge(int age);

    // Find users by age group
    List<User> findByAgeGroup(AgeGroup ageGroup);

    // Find users within age range
    List<User> findByAgeBetween(int minAge, int maxAge);

    // Find users by age group and role
    List<User> findByAgeGroupAndRole(AgeGroup ageGroup, Role role);

    // Find users by age group within a family
    List<User> findByAgeGroupAndFamilyId(AgeGroup ageGroup, Long familyId);

    // Find users by age and family
    List<User> findByAgeAndFamilyId(int age, Long familyId);

    // Find users older than specific age
    List<User> findByAgeGreaterThan(int age);

    // Find users younger than specific age
    List<User> findByAgeLessThan(int age);

    // Find users by age range and role
    List<User> findByAgeBetweenAndRole(int minAge, int maxAge, Role role);

    // Custom query to find top performers by age group
    @Query("SELECT u FROM User u WHERE u.ageGroup = :ageGroup ORDER BY u.coins DESC")
    List<User> findTopPerformersByAgeGroup(@Param("ageGroup") AgeGroup ageGroup);

    // Custom query to find users by age group with streak greater than specified value
    @Query("SELECT u FROM User u WHERE u.ageGroup = :ageGroup AND u.currentStreak > :minStreak")
    List<User> findByAgeGroupWithStreakGreaterThan(@Param("ageGroup") AgeGroup ageGroup,
                                                   @Param("minStreak") int minStreak);

    // Count users by age group
    @Query("SELECT COUNT(u) FROM User u WHERE u.ageGroup = :ageGroup")
    long countByAgeGroup(@Param("ageGroup") AgeGroup ageGroup);

    // Find all distinct age groups in the system
    @Query("SELECT DISTINCT u.ageGroup FROM User u")
    List<AgeGroup> findDistinctAgeGroups();

    // Find users by multiple age groups
    List<User> findByAgeGroupIn(List<AgeGroup> ageGroups);

    // Find leaderboard by age group (top 10 by coins)
    List<User> findTop10ByAgeGroupOrderByCoinsDesc(AgeGroup ageGroup);

    // Find leaderboard by age group (top 10 by streak)
    List<User> findTop10ByAgeGroupOrderByHighestStreakDesc(AgeGroup ageGroup);

    boolean existsByReferralCode(String code);

    Optional<User> findByReferralCode(String trim);

    // Using JPA query with proper field names
    // Using JPA query with proper field names
    //Get top users by current streak
    @Query("SELECT u FROM User u WHERE u.currentStreak > 0 ORDER BY u.currentStreak DESC, u.createdDateTime ASC")
    List<User> findTop11ByOrderByCurrentStreakDescCreatedDateTimeAsc();

    // Get all users with streaks for ranking calculation
    @Query("SELECT u FROM User u WHERE u.currentStreak > 0 ORDER BY u.currentStreak DESC, u.createdDateTime ASC")
    List<User> findAllUsersWithStreaks();

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);

    List<User> findByFamilyAndRelationship(Family family, Relationship relationship);

    boolean existsByReferralCodeIgnoreCase(String trim);

    @Query("""
SELECT u
FROM User u
WHERE u.relationship = com.superme.enums.Relationship.CHILD
AND u.ageGroup = :ageGroup
AND u.currentStreak > 0
ORDER BY u.currentStreak DESC, u.createdDateTime ASC
""")
    List<User> findLeaderboardUsersByAgeGroup(@Param("ageGroup") AgeGroup ageGroup);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);



}
