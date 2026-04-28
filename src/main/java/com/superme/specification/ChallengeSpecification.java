package com.superme.specification;

import com.superme.model.Challenge;
import com.superme.enums.*;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ChallengeSpecification {

    public static Specification<Challenge> filter(
            String name,
            String description,
            Category category,
            String topic,
            Difficulty difficulty,
            Status status,
            List<AgeGroup> ageGroups,
            Integer minCoins,
            Integer maxCoins,
            Integer coinsForCorrectAnswer,
            Integer minTrophies,
            Integer maxTrophies,
            String timeDuration,
            SectionTitle sectionTitle,
            AnswerType answerType,
            Boolean enabled,
            LocalDate createdAfter,
            LocalDate createdBefore,
            LocalDate updatedAfter,
            LocalDate updatedBefore,
            String sortBy,
            String sortDir) {

        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            // Name filter - case insensitive search
            if (name != null && !name.trim().isEmpty()) {
                predicate = cb.and(predicate,
                        cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }

            // Description filter - case insensitive search
            if (description != null && !description.trim().isEmpty()) {
                predicate = cb.and(predicate,
                        cb.like(cb.lower(root.get("description")), "%" + description.toLowerCase() + "%"));
            }

            // Category filter
            if (category != null) {
                predicate = cb.and(predicate, cb.equal(root.get("category"), category));
            }

            // Topic filter - case insensitive search
            if (topic != null && !topic.trim().isEmpty()) {
                predicate = cb.and(predicate,
                        cb.like(cb.lower(root.get("topic")), "%" + topic.toLowerCase() + "%"));
            }

            // Difficulty filter
            if (difficulty != null) {
                predicate = cb.and(predicate, cb.equal(root.get("difficulty"), difficulty));
            }

            // Status filter
            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }

            // Age Groups filter - check if challenge has any of the specified age groups
            if (ageGroups != null && !ageGroups.isEmpty()) {
                Join<Challenge, AgeGroup> ageGroupsJoin = root.join("ageGroups", JoinType.INNER);
                predicate = cb.and(predicate, ageGroupsJoin.in(ageGroups));
                // Remove duplicates by adding distinct
                query.distinct(true);
            }

            // Coins range filter
            if (minCoins != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("coins"), minCoins));
            }
            if (maxCoins != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("coins"), maxCoins));
            }

            // Coins for correct answer filter
            if (coinsForCorrectAnswer != null) {
                predicate = cb.and(predicate, cb.equal(root.get("coinsForCorrectAnswer"), coinsForCorrectAnswer));
            }

            // Trophies range filter
            if (minTrophies != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("trophies"), minTrophies));
            }
            if (maxTrophies != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("trophies"), maxTrophies));
            }

            // Time Duration filter
            if (timeDuration != null) {
                predicate = cb.and(predicate, cb.equal(root.get("timeDuration"), timeDuration));
            }

            // Section Title filter
            if (sectionTitle != null) {
                predicate = cb.and(predicate, cb.equal(root.get("sectionTitle"), sectionTitle));
            }

            // Answer Type filter
            if (answerType != null) {
                predicate = cb.and(predicate, cb.equal(root.get("answerType"), answerType));
            }

            // Enabled filter
            if (enabled != null) {
                predicate = cb.and(predicate, cb.equal(root.get("enabled"), enabled));
            }

            // Created date range filter
            if (createdAfter != null) {
                predicate = cb.and(predicate,
                        cb.greaterThanOrEqualTo(root.get("createdAt"), createdAfter.atStartOfDay()));
            }
            if (createdBefore != null) {
                predicate = cb.and(predicate,
                        cb.lessThanOrEqualTo(root.get("createdAt"), createdBefore.atTime(23, 59, 59)));
            }

            // Updated date range filter
            if (updatedAfter != null) {
                predicate = cb.and(predicate,
                        cb.greaterThanOrEqualTo(root.get("updatedAt"), updatedAfter.atStartOfDay()));
            }
            if (updatedBefore != null) {
                predicate = cb.and(predicate,
                        cb.lessThanOrEqualTo(root.get("updatedAt"), updatedBefore.atTime(23, 59, 59)));
            }

            return predicate;
        };
    }
}