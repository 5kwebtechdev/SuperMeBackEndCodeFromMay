package com.superme.specification;

import com.superme.model.Course;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class CourseSpecification {

    public static Specification<Course> filterCourses(
            String courseName,
            String description,
            String category,
            String difficulty,
            String ageGroup,
            String format,
            String status,
            Integer duration,
            Integer totalCoins
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (courseName != null && !courseName.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("courseName")), "%" + courseName.toLowerCase() + "%"));
            }

            if (description != null && !description.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("description")), "%" + description.toLowerCase() + "%"));
            }

            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            if (difficulty != null) {
                predicates.add(cb.equal(root.get("difficulty"), difficulty));
            }

            if (ageGroup != null) {
                predicates.add(cb.equal(root.get("ageGroup"), ageGroup));
            }

            if (format != null) {
                predicates.add(cb.equal(root.get("format"), format));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (duration != null) {
                predicates.add(cb.equal(root.get("duration"), duration));
            }

            if (totalCoins != null) {
                predicates.add(cb.equal(root.get("totalCoins"), totalCoins));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
