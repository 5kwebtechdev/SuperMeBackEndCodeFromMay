package com.superme.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.superme.model.JournalEntry;
import com.superme.model.User;

/**
 * Repository interface for JournalEntry entity operations.
 * 
 * Provides data access methods for journal entries with custom queries
 * for admin dashboard analytics.
 * 
 * @author MindfullB Admin Team
 * @version 1.0
 */
@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

        // ============================================================================
        // BASIC QUERY METHODS
        // ============================================================================

        List<JournalEntry> findByCreatedBy(User createdBy);

        List<JournalEntry> findByCreatedById(Long createdById);



        Long countByCreatedById(Long createdById);



        // ============================================================================
        // ADMIN ANALYTICS QUERIES
        // ============================================================================

        @Query("SELECT j.createdBy.id, COUNT(j) FROM JournalEntry j GROUP BY j.createdBy.id")
        List<Object[]> countJournalEntriesByUser();


        List<JournalEntry> findByCreatedByAndCreationDate(User user, LocalDate today);

        List<JournalEntry> findByCreatedByAndCreationDateBetween(User user, LocalDate start, LocalDate end);

        boolean existsByCreatedByAndCreationDate(User user, LocalDate today);

        @Query("SELECT MIN(j.creationDate) FROM JournalEntry j WHERE j.createdBy.id = :userId")
        Optional<LocalDate> findFirstJournalEntryDateByUserId(@Param("userId") Long userId);

        @Query("SELECT MAX(j.creationDate) FROM JournalEntry j WHERE j.createdBy.id = :userId")
        Optional<LocalDate> findLastJournalEntryDateByUserId(@Param("userId") Long userId);
}