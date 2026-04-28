package com.superme.repository;

import com.superme.model.CoinTransaction;
import com.superme.model.Habit;
import com.superme.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CoinTransactionRepository extends JpaRepository<CoinTransaction, Long> {

    // Existing method
    List<CoinTransaction> findByUser(User user);

    // Add this missing method
    List<CoinTransaction> findByUserAndTimestampBetween(User user, LocalDateTime start, LocalDateTime end);

    // Optional: Additional useful methods
    List<CoinTransaction> findByUserAndType(User user, String type);

    List<CoinTransaction> findByUserAndTypeAndTimestampBetween(User user, String type, LocalDateTime start, LocalDateTime end);

    @Query("SELECT SUM(ct.amount) FROM CoinTransaction ct WHERE ct.user = :user AND ct.type = :type AND ct.timestamp BETWEEN :start AND :end")
    Integer sumAmountByUserAndTypeAndTimestampBetween(@Param("user") User user,
                                                      @Param("type") String type,
                                                      @Param("start") LocalDateTime start,
                                                      @Param("end") LocalDateTime end);

    //    List<CoinTransaction> findByUserAndTimestampBetween(User user, LocalDateTime start, LocalDateTime end);}
// Method to delete transactions by Habit
    @Modifying
    @Query("DELETE FROM CoinTransaction ct WHERE ct.habit = :habit")
    void deleteByHabit(@Param("habit") Habit habit);

    // Alternative method using habit ID
    @Modifying
    @Query("DELETE FROM CoinTransaction ct WHERE ct.habit.id = :habitId")
    void deleteByHabitId(@Param("habitId") Long habitId);

}

