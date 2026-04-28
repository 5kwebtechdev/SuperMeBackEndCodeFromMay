package com.superme.repository;

import com.superme.model.FeatureUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeatureUsageRepository
        extends JpaRepository<FeatureUsage, Long> {
}

