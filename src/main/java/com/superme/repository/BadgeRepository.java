package com.superme.repository;

import aj.org.objectweb.asm.commons.Remapper;
import com.superme.model.Badge;
import com.superme.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BadgeRepository extends JpaRepository<Badge, Long> {

    // Find all badges earned by a specific user
    List<Badge> findByUser(User user);
    
    // Alternative method using user ID
    List<Badge> findByUserId(Long userId);

    List<Badge> findByUserIdOrderByEarnedAtDesc(Long userId);
    boolean existsByUserAndName(User user, String name);
    int countByUserId(Long userId);

    @Query("SELECT b FROM Badge b WHERE b.user.id = :userId AND b.name = :badgeName")
    List<Badge> findByUserIdAndName(Long userId, String badgeName);

    List<Badge> findByUserAndEarnedAtAfterOrderByEarnedAtDesc(User user, LocalDateTime dateTime);

    // Add this new method to check if user has earned any level of a badge type
    @Query("SELECT COUNT(b) > 0 FROM Badge b WHERE b.user = :user AND b.badgeType = :badgeType")
    boolean existsByUserAndBadgeType(@Param("user") User user, @Param("badgeType") String badgeType);

    // Add this method to find badges by badge type
    List<Badge> findByUserAndBadgeType(User user, String badgeType);

    List<Badge> findByUserOrderByEarnedAtDesc(User user);

    Optional<Badge> findFirstByUserIdAndPopupShownFalseOrderByEarnedAtDesc(Long userId);

    Optional<Badge> findByIdAndUserIdAndPopupShownFalse(Long badgeId, Long userId);
}