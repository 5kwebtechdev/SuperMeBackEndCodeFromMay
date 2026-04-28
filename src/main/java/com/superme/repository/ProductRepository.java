package com.superme.repository;

import com.superme.model.Product;
import com.superme.model.School;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findTop10BySchoolAndClassNameAndMarkAsMandatoryItemTrueOrderByIdDesc(
            School school, String className);

    List<Product> findTop10BySchoolAndClassNameOrderByIdDesc(
            School school, String className);

    Page<Product> findBySchoolAndClassName(School school, String className, Pageable pageable);

    Page<Product> findBySchoolAndClassNameAndCategoryContainingIgnoreCase(
            School school, String className, String category, Pageable pageable);

    Page<Product> findBySchoolAndClassNameAndCategoryContainingIgnoreCaseAndProductNameContainingIgnoreCase(
            School school, String className, String category, String search, Pageable pageable);

    Page<Product> findBySchoolAndClassNameAndProductNameContainingIgnoreCase(
            School school, String className, String search, Pageable pageable);

    Optional<Product> findByIdAndSchool(Long id, School school);
}
