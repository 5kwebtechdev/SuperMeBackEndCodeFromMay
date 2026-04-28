package com.superme.repository;

import com.superme.model.BadgeProgress;
import com.superme.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BadgeProgressRepository extends JpaRepository<BadgeProgress, Long> {
    Optional<BadgeProgress> findByUserAndBadgeName(User user, String badgeName);
    List<BadgeProgress> findByUser(User user);
    List<BadgeProgress> findByUserAndCompleted(User user, boolean completed);

    @Query("SELECT bp FROM BadgeProgress bp WHERE bp.user.id = :userId AND bp.completed = false")
    List<BadgeProgress> findIncompleteProgressByUserId(Long userId);
}
