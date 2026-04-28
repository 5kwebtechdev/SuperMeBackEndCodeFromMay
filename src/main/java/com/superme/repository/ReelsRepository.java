package com.superme.repository;

import com.superme.model.Reels;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Reels entity operations.
 */
@Repository
public interface ReelsRepository extends JpaRepository<Reels, Long> {

    // ============================================================================
    // STATISTICS QUERIES
    // ============================================================================

    @Query("SELECT COUNT(r) FROM Reels r")
    Long countTotalReels();

    @Query("SELECT r.status, COUNT(r) FROM Reels r GROUP BY r.status")
    List<Object[]> countReelsByStatus();

    @Query("SELECT r.category, COUNT(r) FROM Reels r GROUP BY r.category")
    List<Object[]> countReelsByCategory();

    // ============================================================================
    // FILTER OPTIONS QUERIES
    // ============================================================================

    @Query("SELECT DISTINCT r.category FROM Reels r WHERE r.category IS NOT NULL ORDER BY r.category")
    List<String> findDistinctCategories();

    // ============================================================================
    // FILTERING QUERIES
    // ============================================================================

    List<Reels> findByCategory(String category);

    List<Reels> findByStatus(Reels.ApprovalStatus status);

    @Query("SELECT r FROM Reels r WHERE " +
           "(:category IS NULL OR r.category = :category) AND " +
           "(:status IS NULL OR r.status = :status)")
    List<Reels> findWithFilters(String category, Reels.ApprovalStatus status);


    @Query("SELECT COUNT(r) FROM Reels r WHERE r.createdBy = :userId")
    int countReelsByUser(Long userId);

    @Query("SELECT r FROM Reels r WHERE r.createdBy = :userId")
    List<Reels> findReelsByUser(Long userId);
}
