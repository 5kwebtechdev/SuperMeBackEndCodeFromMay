package com.superme.repository;

import com.superme.model.School;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolRepository extends JpaRepository<School, Long> {

    Page<School> findBySchoolNameContainingIgnoreCaseOrSchoolBranchContainingIgnoreCaseOrCityContainingIgnoreCase(
            String name, String schoolBranch, String city, Pageable pageable);
}
