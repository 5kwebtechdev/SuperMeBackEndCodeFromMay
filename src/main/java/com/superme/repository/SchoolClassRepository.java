package com.superme.repository;

import com.superme.model.SchoolClass;
import com.superme.model.School;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {

    List<SchoolClass> findBySchool(School school);
}
