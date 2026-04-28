
package com.superme.repository;

import com.superme.model.Tutor;
import org.springframework.beans.PropertyValues;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.superme.enums.FeeType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TutorRepository extends JpaRepository<Tutor, Long> {


        @Query("""
    SELECT DISTINCT t
    FROM Tutor t
    LEFT JOIN t.subjects s
    LEFT JOIN t.contactModes cm
    LEFT JOIN t.levels l
    WHERE 
        (:subject IS NULL OR s = :subject)
    AND (:mode IS NULL OR cm = :mode)
    AND (:standard IS NULL OR l = :standard)
    AND (:levels IS NULL OR l IN :levels)
    AND (:location IS NULL OR LOWER(t.location) LIKE LOWER(CONCAT('%', :location, '%')))
    AND (:start IS NULL OR t.startTime >= :start)
    AND (:end IS NULL OR t.endTime <= :end)
""")
        List<Tutor> findTutorsWithAllFilters(
                @Param("subject") Tutor.Subject subject,
                @Param("mode") Tutor.ContactMode mode,
                @Param("standard") String standard,
                @Param("levels") List<String> levels,
                @Param("location") String location,
                @Param("start") LocalDateTime start,
                @Param("end") LocalDateTime end
        );




        Optional<Tutor> findByEmail(String email);

        List<Tutor> findByIsActiveTrue();
        List<Tutor> findByIsVerifiedTrue();
        List<Tutor> findByIsActiveTrueAndIsVerifiedTrue();

        List<Tutor> findByIsActive(Boolean isActive);
        List<Tutor> findByIsVerified(Boolean isVerified);
        List<Tutor> findByIsVerifiedFalse();

        long countByIsActive(Boolean isActive);
        long countByIsVerified(Boolean isVerified);

        default long countByIsActiveTrue() {
                return countByIsActive(true);
        }

        default long countByIsVerifiedTrue() {
                return countByIsVerified(true);
        }

        List<Tutor> findByGender(Tutor.Gender gender);
        List<Tutor> findByExperience(Tutor.Experience experience);
        List<Tutor> findByEntityType(Tutor.EntityType entityType);

        List<Tutor> findByLocationContainingIgnoreCase(String location);

        @Query("SELECT DISTINCT t FROM Tutor t JOIN t.subjects s WHERE s = :subject")
        List<Tutor> findBySubject(@Param("subject") Tutor.Subject subject);

        @Query("""
        SELECT DISTINCT t
        FROM Tutor t
        WHERE (:subject IS NULL OR :subject MEMBER OF t.subjects)
          AND (:mode IS NULL OR :mode MEMBER OF t.contactModes)
          AND (:standard IS NULL OR :standard MEMBER OF t.levels)
          AND (:location IS NULL OR LOWER(t.location) LIKE LOWER(CONCAT('%', :location, '%')))
    """)
        List<Tutor> findTutorsWithFilters(
                @Param("subject") Tutor.Subject subject,
                @Param("mode") Tutor.ContactMode mode,
                @Param("standard") String standard,
                @Param("location") String location
        );

        List<Tutor> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);

        @Query("SELECT DISTINCT t FROM Tutor t JOIN t.levels l WHERE l IN :levels")
        List<Tutor> findByLevelsIn(@Param("levels") List<String> levels);

        @Query("SELECT DISTINCT t FROM Tutor t JOIN t.levels l WHERE l = :level")
        List<Tutor> findByLevel(@Param("level") String level);

        List<Tutor> findByFeeType(FeeType feeType);
        @Query("SELECT DISTINCT t FROM Tutor t JOIN t.subjects s WHERE s IN :subjects")
        List<Tutor> findBySubjectsIn(@Param("subjects") List<Tutor.Subject> subjects);
        List<Tutor> findByFeesBetween(BigDecimal minFees, BigDecimal maxFees);
        List<Tutor> findByFeesLessThanEqual(BigDecimal maxFees);
        List<Tutor> findByFeesGreaterThanEqual(BigDecimal minFees);
}