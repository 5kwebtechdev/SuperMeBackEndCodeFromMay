package com.superme.specification;

import com.superme.model.Task;
import com.superme.model.User;
import com.superme.model.Family;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

public class TaskSpecification {

    public static Specification<Task> filter(
            String title,
            String description,
            Task.TaskStatus status,
            Task.Priority priority,
            String routine,
            Boolean everyday,
            Boolean everyWeekend,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate createdAt,
            LocalDate updatedAt,
            Integer rewardCoins,
            Set<String> tags,
            Set<DayOfWeek> daysOfWeek,
            User createdBy,
            Family family) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            // Security: Always filter by user - user can only see their own tasks
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

            if (rewardCoins != null) {
                predicate = cb.and(predicate, cb.equal(root.get("rewardCoins"), rewardCoins));
            }

            // Handle tags collection properly
            if (tags != null && !tags.isEmpty()) {
                Join<Task, String> tagsJoin = root.join("tags", JoinType.INNER);
                predicate = cb.and(predicate, tagsJoin.in(tags));
            }

            // Handle daysOfWeek collection properly
            if (daysOfWeek != null && !daysOfWeek.isEmpty()) {
                Join<Task, DayOfWeek> daysJoin = root.join("daysOfWeek", JoinType.INNER);
                predicate = cb.and(predicate, daysJoin.in(daysOfWeek));
            }

            return predicate;
        };
    }
}