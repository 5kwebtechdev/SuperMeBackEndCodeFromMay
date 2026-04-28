package com.superme.repository;

import com.superme.model.Mood;
import com.superme.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MoodRepository extends JpaRepository<Mood, Long> {
    Optional<Mood> findByUserAndDateAndTime(User user, LocalDate date, LocalTime time);

    int countByUserIdAndDate(Long userId, LocalDate today);

    Optional<Mood> findTopByUserOrderByDateDescTimeDesc(User user);

    List<Mood> findByUserIdAndDateOrderByTimeAsc(Long userId, LocalDate date);

    @Query("SELECT m FROM Mood m WHERE m.user = :user AND m.date BETWEEN :startDate AND :endDate ORDER BY m.date ASC, m.time ASC")
    List<Mood> findByUserAndDateBetweenOrderByDateAscTimeAsc(
            @Param("user") User user,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}