package com.superme.repository;

import com.superme.model.MoodMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MoodMetadataRepository extends JpaRepository<MoodMetadata, Long> {
    Optional<MoodMetadata> findByMoodAndAudience(String mood, String audience);
}
