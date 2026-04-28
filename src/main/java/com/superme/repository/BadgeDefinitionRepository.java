package com.superme.repository;

import com.superme.enums.AgeGroup;
import com.superme.model.BadgeDefinition;
import com.superme.enums.BadgeType;
import com.superme.enums.BadgeRarity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BadgeDefinitionRepository extends JpaRepository<BadgeDefinition, String> {
    List<BadgeDefinition> findByTriggerEventAndActiveTrue(String triggerEvent);

    List<BadgeDefinition> findByBadgeTypeAndActiveTrue(BadgeType badgeType);

    List<BadgeDefinition> findByActiveTrue();

    List<BadgeDefinition> findByRarityAndActiveTrue(BadgeRarity rarity);

    // ADD THIS METHOD - It fetches age groups eagerly
    @Query("SELECT DISTINCT bd FROM BadgeDefinition bd LEFT JOIN FETCH bd.applicableAgeGroups WHERE bd.active = true")
    List<BadgeDefinition> findByActiveTrueWithAgeGroups();

    // Keep your existing method for age filtering
    @Query("SELECT bd FROM BadgeDefinition bd WHERE bd.active = true AND :ageGroup MEMBER OF bd.applicableAgeGroups")
    List<BadgeDefinition> findByActiveTrueAndApplicableAgeGroupsContaining(@Param("ageGroup") AgeGroup ageGroup);

//    List<BadgeDefinition> findByNameIn(List<String> names);

    // ADD THIS METHOD - Find by name (returns Optional for safety)
    Optional<BadgeDefinition> findByName(String name);

    @Query("SELECT b FROM BadgeDefinition b WHERE b.name IN :names AND b.active = true")
    List<BadgeDefinition> findByNameIn(@Param("names") List<String> names);

}

