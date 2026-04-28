package com.superme.repository;

import com.superme.model.FieldOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FieldOptionRepository extends JpaRepository<FieldOption, Long> {
    List<FieldOption> findByFieldIdOrderByDisplayOrder(Long fieldId);
    List<FieldOption> findByFieldIdAndIsActiveTrue(Long fieldId);
    Optional<FieldOption> findByFieldIdAndOptionValue(Long fieldId, String optionValue);
}
