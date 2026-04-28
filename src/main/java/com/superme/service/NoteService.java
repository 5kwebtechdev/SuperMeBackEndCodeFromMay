package com.superme.service;

import com.superme.model.Note;
import com.superme.model.User;
import com.superme.repository.NoteRepository;
import com.superme.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class NoteService {

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private UserService userService;

    // Add UserRepository for direct access to user count
    @Autowired
    private UserRepository userRepository;

    /**
     * Create a new note for a user.
     * (sharedWithParents removed)
     */
    public Note addNote(User user, String title, String content) {
        Note note = new Note();
        note.setUser(user);
        note.setTitle(title);
        note.setContent(content);
        java.time.LocalDate nowDate = java.time.LocalDate.now();
        java.time.LocalTime nowTime = java.time.LocalTime.now();
        note.setCreatedDate(nowDate);
        note.setCreatedTime(nowTime);
        note.setUpdatedDate(nowDate);
        note.setUpdatedTime(nowTime);
        return noteRepository.save(note);
    }

    /**
     * Overloaded addNote to support tags.
     */
    public Note addNote(User user, String title, String content, List<String> tags) {
        Note note = new Note();
        note.setUser(user);
        note.setTitle(title);
        note.setContent(content);
        note.setTags(tags);
        java.time.LocalDate nowDate = java.time.LocalDate.now();
        java.time.LocalTime nowTime = java.time.LocalTime.now();
        note.setCreatedDate(nowDate);
        note.setCreatedTime(nowTime);
        note.setUpdatedDate(nowDate);
        note.setUpdatedTime(nowTime);
        return noteRepository.save(note);
    }

    /**
     * Update an existing note for a user (only if the note belongs to the user).
     * (sharedWithParents removed)
     */
    public Note updateNoteForUser(Long noteId, User user, String title, String content) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("Note not found."));
        if (!note.getUser().getId().equals(user.getId())) {
            return null; // Not allowed to update someone else's note
        }
        note.setTitle(title);
        note.setContent(content);
        note.setUpdatedDate(java.time.LocalDate.now());
        note.setUpdatedTime(java.time.LocalTime.now());
        return noteRepository.save(note);
    }

    /**
     * Overloaded updateNoteForUser to support tags.
     */
    public Note updateNoteForUser(Long noteId, User user, String title, String content, List<String> tags) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("Note not found."));
        if (!note.getUser().getId().equals(user.getId())) {
            return null; // Not allowed to update someone else's note
        }
        note.setTitle(title);
        note.setContent(content);
        note.setTags(tags);
        note.setUpdatedDate(java.time.LocalDate.now());
        note.setUpdatedTime(java.time.LocalTime.now());
        return noteRepository.save(note);
    }

    /**
     * Delete a note for a user (only if the note belongs to the user).
     */
    public boolean deleteNoteForUser(Long noteId, User user) {
        return noteRepository.findById(noteId)
                .filter(note -> note.getUser().getId().equals(user.getId()))
                .map(note -> {
                    noteRepository.deleteById(noteId);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Get all notes created by a user.
     */
    public List<Note> getNotesByUserId(Long userId) {
        return noteRepository.findByUserId(userId);
    }

    /**
     * Get total number of notes for a specific user.
     */
    public long getTotalNotesForUser(Long userId) {
        return noteRepository.countByUserId(userId);
    }

    /**
     * Get total number of notes for a specific user by User object.
     */
    public long getTotalNotesForUser(User user) {
        return noteRepository.countByUserId(user.getId());
    }

    /**
     * Get total number of notes across all users in the system.
     */
    public long getTotalNotesForAllUsers() {
        return noteRepository.count();
    }

    /**
     * Get notes count per user (returns Map of userId -> count).
     */
    public Map<Long, Long> getNotesCountByUser() {
        List<Object[]> results = noteRepository.countNotesByUser();
        Map<Long, Long> notesCountMap = new HashMap<>();

        for (Object[] result : results) {
            Long userId = (Long) result[0];
            Long count = (Long) result[1];
            notesCountMap.put(userId, count);
        }

        return notesCountMap;
    }

    /**
     * Get detailed notes statistics for all users.
     */
    public Map<String, Object> getNotesStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalNotes", getTotalNotesForAllUsers());
        stats.put("notesPerUser", getNotesCountByUser());
        stats.put("averageNotesPerUser", calculateAverageNotesPerUser());
        return stats;
    }

    /**
     * Calculate average number of notes per user.
     * Fixed to use UserRepository directly instead of undefined method.
     */
    private double calculateAverageNotesPerUser() {
        try {
            // Use UserRepository.count() instead of userService.getTotalUserCount()
            long totalUsers = userRepository.count();
            long totalNotes = getTotalNotesForAllUsers();

            if (totalUsers == 0) {
                return 0.0;
            }

            return (double) totalNotes / totalUsers;
        } catch (Exception e) {
            System.err.println("Error calculating average notes per user: " + e.getMessage());
            return 0.0;
        }
    }

    /**
     * Get users with most notes (top N users).
     * Updated to use Pageable for Spring Data compatibility.
     */
    public List<Object[]> getTopUsersWithMostNotes(int limit) {
        // Use PageRequest for limiting results
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limit);
        return noteRepository.findTopUsersWithMostNotes(pageable);
    }

    /**
     * Get recent notes count for a user (last N days).
     * Updated to use createdDate and createdTime fields.
     */
    public long getRecentNotesCountForUser(Long userId, int days) {
        java.time.LocalDate nowDate = java.time.LocalDate.now();
        java.time.LocalTime nowTime = java.time.LocalTime.now();
        java.time.LocalDate cutoffDate = nowDate.minusDays(days);
        java.time.LocalTime cutoffTime = nowTime; // Or LocalTime.MIN for start of day
        return noteRepository.countByUserIdAndCreatedDateTimeAfter(userId, cutoffDate, cutoffTime);
    }

    /**
     * Get all notes for admin overview (with user information).
     */
    public List<Note> getAllNotesForAdmin() {
        return noteRepository.findAllWithUser();
    }

    /**
     * Get total user count (helper method for statistics).
     * This method provides the functionality that was expected from UserService.
     */
    public long getTotalUserCount() {
        return userRepository.count();
    }

    /**
     * Enhanced notes statistics with additional metrics.
     */
    public Map<String, Object> getEnhancedNotesStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();

            long totalNotes = getTotalNotesForAllUsers();
            long totalUsers = getTotalUserCount();
            Map<Long, Long> notesPerUser = getNotesCountByUser();

            stats.put("totalNotes", totalNotes);
            stats.put("totalUsers", totalUsers);
            stats.put("notesPerUser", notesPerUser);
            stats.put("averageNotesPerUser", calculateAverageNotesPerUser());
            stats.put("usersWithNotes", notesPerUser.size());
            stats.put("usersWithoutNotes", totalUsers - notesPerUser.size());

            // Calculate additional metrics
            if (!notesPerUser.isEmpty()) {
                stats.put("maxNotesPerUser",
                        notesPerUser.values().stream().mapToLong(Long::longValue).max().orElse(0L));
                stats.put("minNotesPerUser",
                        notesPerUser.values().stream().mapToLong(Long::longValue).min().orElse(0L));
            }

            return stats;
        } catch (Exception e) {
            System.err.println("Error calculating enhanced notes statistics: " + e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Find notes by tag for a user.
     */
    public List<Note> getNotesByUserIdAndTag(Long userId, String tag) {
        return noteRepository.findByUserIdAndTagsContaining(userId, tag);
    }

    /**
     * Search notes by title, content, or tag for a user.
     */
    public List<Note> searchNotesByUser(Long userId, String query) {
        return noteRepository.searchNotesByUserAndQuery(userId, query);
    }
}