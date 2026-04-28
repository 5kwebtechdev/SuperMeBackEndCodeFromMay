package com.superme.specification;

import com.superme.model.Habit;
import com.superme.model.User;
import com.superme.model.Family;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import java.time.DayOfWeek;
import java.time.LocalDate; // ✅ ADD: Import LocalDate
import java.util.Set;

public class HabitSpecification {

    public static Specification<Habit> filter(
            String title,
            String description,
            Habit.HabitStatus status,
            Habit.Priority priority,
            Habit.Routine routine,
            Boolean everyday,
            Boolean everyWeekend,
            LocalDate startDate, // ✅ FIXED: Changed from Long to LocalDate
            LocalDate endDate, // ✅ FIXED: Changed from Long to LocalDate
            LocalDate createdAt, // ✅ FIXED: Changed from Long to LocalDate
            LocalDate updatedAt, // ✅ FIXED: Changed from Long to LocalDate
            Integer coinReward,
            Set<String> tags,
            Set<DayOfWeek> daysOfWeek,
            User createdBy,
            Family family) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            // ✅ SECURITY: Always filter by user - user can only see their own habits
            if (createdBy != null) {
                predicate = cb.and(predicate, cb.equal(root.get("createdBy"), createdBy));
            }

            if (title != null && !title.trim().isEmpty()) {
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%"));
            }

            if (description != null && !description.trim().isEmpty()) {
                predicate = cb.and(predicate,
                        cb.like(cb.lower(root.get("description")), "%" + description.toLowerCase() + "%"));
            }

            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }

            if (priority != null) {
                predicate = cb.and(predicate, cb.equal(root.get("priority"), priority));
            }

            if (routine != null) {
                predicate = cb.and(predicate, cb.equal(root.get("routine"), routine));
            }

            if (everyday != null) {
                predicate = cb.and(predicate, cb.equal(root.get("everyday"), everyday));
            }

            if (everyWeekend != null) {
                predicate = cb.and(predicate, cb.equal(root.get("everyWeekend"), everyWeekend));
            }

            if (startDate != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("startDate"), startDate));
            }

            if (endDate != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("endDate"), endDate));
            }

            if (createdAt != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("createdAt"), createdAt));
            }

            if (updatedAt != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("updatedAt"), updatedAt));
            }

            if (coinReward != null) {
                predicate = cb.and(predicate, cb.equal(root.get("coinReward"), coinReward));
            }

            // ✅ FIXED: Handle tags collection properly
            if (tags != null && !tags.isEmpty()) {
                Join<Habit, String> tagsJoin = root.join("tags", JoinType.INNER);
                predicate = cb.and(predicate, tagsJoin.in(tags));
            }

            // ✅ FIXED: Handle daysOfWeek collection properly
            if (daysOfWeek != null && !daysOfWeek.isEmpty()) {
                Join<Habit, DayOfWeek> daysJoin = root.join("daysOfWeek", JoinType.INNER);
                predicate = cb.and(predicate, daysJoin.in(daysOfWeek));
            }

            return predicate;
        };
    }
}