package com.superme.repository;

import com.superme.model.MoodTransition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MoodTransitionRepository extends JpaRepository<MoodTransition, Long> {
    MoodTransition findByMoodFromAndMoodToAndParentVersion(String moodFrom, String moodTo, boolean parentVersion);

    @Query("SELECT mt FROM MoodTransition mt " +
            "WHERE (:fromMood IS NULL OR mt.moodFrom = :fromMood) " +
            "AND (:toMood IS NULL OR mt.moodTo = :toMood) " +
            "AND mt.parentVersion = :parentVersion")
    MoodTransition findTransition(
            @Param("fromMood") String fromMood,
            @Param("toMood") String toMood,
            @Param("parentVersion") boolean parentVersion
    );

}
