package com.superme.repository;

import com.superme.model.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {

    // Find all notes by user ID
    List<Note> findByUserId(Long userId);

    // Count methods
    long countByUserId(Long userId);

    @Query("SELECT n.user.id, COUNT(n) FROM Note n GROUP BY n.user.id")
    List<Object[]> countNotesByUser();

    // Top users by note count (use pageable for limiting)
    @Query("SELECT n.user.id, COUNT(n) as noteCount FROM Note n GROUP BY n.user.id ORDER BY noteCount DESC")
    List<Object[]> findTopUsersWithMostNotes(Pageable pageable);

    // Admin queries
    @Query("SELECT n FROM Note n JOIN FETCH n.user ORDER BY n.createdDate DESC, n.createdTime DESC")
    List<Note> findAllWithUser();

    @Query("SELECT n FROM Note n WHERE n.user.id = :userId ORDER BY n.createdDate DESC, n.createdTime DESC")
    List<Note> findByUserIdOrderByCreatedDateDesc(Long userId);

    // Find notes by user and tag
    List<Note> findByUserIdAndTagsContaining(Long userId, String tag);

    // Search notes by user, title, content, or tags (case-insensitive)
    @Query("SELECT n FROM Note n WHERE n.user.id = :userId AND " +
            "(LOWER(n.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(n.content) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR EXISTS (SELECT t FROM n.tags t WHERE LOWER(t) LIKE LOWER(CONCAT('%', :query, '%'))))")
    List<Note> searchNotesByUserAndQuery(Long userId, String query);

    // Count notes by user after a given date and time
    @Query("SELECT COUNT(n) FROM Note n WHERE n.user.id = :userId AND (n.createdDate > :cutoffDate OR (n.createdDate = :cutoffDate AND n.createdTime > :cutoffTime))")
    long countByUserIdAndCreatedDateTimeAfter(Long userId, java.time.LocalDate cutoffDate,
            java.time.LocalTime cutoffTime);

    long countByCreatedDate(java.time.LocalDate createdDate);
}