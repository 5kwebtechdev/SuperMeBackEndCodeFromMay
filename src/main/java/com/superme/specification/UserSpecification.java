package com.superme.specification;

import com.superme.dto.UserSearchRequestDto;
import com.superme.model.User;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class UserSpecification {

    private UserSpecification() { /* utility */ }

    public static Specification<User> fromRequest(UserSearchRequestDto req) {
        return (root, query, cb) -> {
            // When joining to collections, ensure distinct results
            if (requiresJoinForExistence(req)) {
                query.distinct(true);
            }

            List<Predicate> predicates = new ArrayList<>();

            // ─────────── Exact / ID based ───────────
            if (req.getId() != null) {
                predicates.add(cb.equal(root.get("id"), req.getId()));
            }
            if (req.getIds() != null && !req.getIds().isEmpty()) {
                predicates.add(root.get("id").in(req.getIds()));
            }

            // ─────────── String LIKE filters ───────────
            addLikePredicate(cb, root, predicates, "name", req.getName());
            addLikePredicate(cb, root, predicates, "email", req.getEmail());
            addLikePredicate(cb, root, predicates, "phone", req.getPhone());

            // ─────────── Categorical / List filters ───────────
            if (req.getGenders() != null && !req.getGenders().isEmpty()) {
                predicates.add(root.get("gender").in(req.getGenders()));
            }

            if (req.getRoles() != null && !req.getRoles().isEmpty()) {
                predicates.add(root.get("role").in(req.getRoles()));
            }

            if (req.getRelationships() != null && !req.getRelationships().isEmpty()) {
                predicates.add(root.get("relationship").in(req.getRelationships()));
            }

            if (req.getAgeGroups() != null && !req.getAgeGroups().isEmpty()) {
                predicates.add(root.get("ageGroup").in(req.getAgeGroups()));
            }

            // ─────────── DOB range ───────────
            if (req.getDobFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dateOfBirth"), req.getDobFrom()));
            }
            if (req.getDobTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dateOfBirth"), req.getDobTo()));
            }

            // ─────────── Age range (derived column) ───────────
            if (req.getMinAge() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("age"), req.getMinAge()));
            }
            if (req.getMaxAge() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("age"), req.getMaxAge()));
            }

            // ─────────── Numeric ranges ───────────
            addRangePredicate(cb, root, predicates, "coins", req.getMinCoins(), req.getMaxCoins());
            addRangePredicate(cb, root, predicates, "currentStreak", req.getMinCurrentStreak(), req.getMaxCurrentStreak());
            addRangePredicate(cb, root, predicates, "highestStreak", req.getMinHighestStreak(), req.getMaxHighestStreak());

            // ─────────── Boolean flags ───────────
            if (req.getIsLoggedIn() != null) {
                predicates.add(cb.equal(root.get("isLoggedIn"), req.getIsLoggedIn()));
            }
            if (req.getEmailVerified() != null) {
                predicates.add(cb.equal(root.get("emailVerified"), req.getEmailVerified()));
            }
            if (req.getEnabled() != null) {
                predicates.add(cb.equal(root.get("enabled"), req.getEnabled()));
            }

            // ─────────── DateTime ranges ───────────
            if (req.getLastLoginFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("lastLoginDate"), req.getLastLoginFrom()));
            }
            if (req.getLastLoginTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("lastLoginDate"), req.getLastLoginTo()));
            }
            if (req.getCreatedFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdDateTime"), req.getCreatedFrom()));
            }
            if (req.getCreatedTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdDateTime"), req.getCreatedTo()));
            }

            // ─────────── ManyToOne relations (by id or list-of-ids) ───────────
            if (req.getFamilyId() != null) {
                predicates.add(cb.equal(root.get("family").get("id"), req.getFamilyId()));
            }
            if (req.getFamilyIds() != null && !req.getFamilyIds().isEmpty()) {
                predicates.add(root.get("family").get("id").in(req.getFamilyIds()));
            }

            if (req.getAvatarId() != null) {
                predicates.add(cb.equal(root.get("avatar").get("id"), req.getAvatarId()));
            }
            if (req.getAvatarIds() != null && !req.getAvatarIds().isEmpty()) {
                predicates.add(root.get("avatar").get("id").in(req.getAvatarIds()));
            }

            if (req.getPetId() != null) {
                predicates.add(cb.equal(root.get("pet").get("id"), req.getPetId()));
            }
            if (req.getPetIds() != null && !req.getPetIds().isEmpty()) {
                predicates.add(root.get("pet").get("id").in(req.getPetIds()));
            }

            // ─────────── OneToMany existence checks (no deep child field search) ───────────
            if (req.getHasCalendarEvents() != null) {
                predicates.add(existencePredicate(cb, root, "calendarEvents", req.getHasCalendarEvents()));
            }
            if (req.getHasCoinHistory() != null) {
                predicates.add(existencePredicate(cb, root, "coinHistory", req.getHasCoinHistory()));
            }
            if (req.getHasRewards() != null) {
                predicates.add(existencePredicate(cb, root, "rewards", req.getHasRewards()));
            }
            if (req.getHasBadges() != null) {
                predicates.add(existencePredicate(cb, root, "badges", req.getHasBadges()));
            }
            if (req.getHasNotes() != null) {
                predicates.add(existencePredicate(cb, root, "notes", req.getHasNotes()));
            }

            // combine
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // ─────────── Helpers ───────────

    private static void addLikePredicate(CriteriaBuilder cb, Root<User> root, List<Predicate> predicates,
                                         String fieldName, String value) {
        if (value != null && !value.isBlank()) {
            predicates.add(cb.like(cb.lower(root.get(fieldName)), "%" + value.toLowerCase() + "%"));
        }
    }

    private static void addRangePredicate(CriteriaBuilder cb, Root<User> root, List<Predicate> predicates,
                                          String fieldName, Integer min, Integer max) {
        if (min != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get(fieldName), min));
        }
        if (max != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get(fieldName), max));
        }
    }

    /**
     * Produces a predicate that checks existence (size > 0) of a collection attribute.
     * If want existence = true => join and check is not null / size > 0
     * existence = false => check is empty via subquery / not exists join
     */
    private static Predicate existencePredicate(CriteriaBuilder cb, Root<User> root, String collectionAttr, Boolean existence) {
        if (Boolean.TRUE.equals(existence)) {
            // simple join and non-null check ensures user has at least one related element
            Join<Object, Object> join = root.join(collectionAttr, JoinType.LEFT);
            return cb.isNotNull(join.get("id")); // collection element has id
        } else {
            // check that collection is empty: use size == 0
            return cb.equal(cb.size(root.get(collectionAttr)), 0);
        }
    }

    private static boolean requiresJoinForExistence(UserSearchRequestDto req) {
        return Boolean.TRUE.equals(req.getHasCalendarEvents())
                || Boolean.TRUE.equals(req.getHasCoinHistory())
                || Boolean.TRUE.equals(req.getHasRewards())
                || Boolean.TRUE.equals(req.getHasBadges())
                || Boolean.TRUE.equals(req.getHasNotes());
    }
}

