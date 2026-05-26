package com.superme.repository;

import com.superme.model.Family;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FamilyRepository extends JpaRepository<Family, Long> {
    Optional<Family> findByFamilyCode(String familyCode);

    boolean existsByFamilyCode(String code);

    boolean existsByFamilyName(String familyName);

    Optional<Family> findByFamilyName(String familyName);

    @Modifying
    @Transactional
    @Query("UPDATE Family f SET f.createdBy = :userId WHERE f.familyCode = :familyCode")
    int updateUserIdByFamilyCode(@Param("familyCode") String familyCode,
                                 @Param("userId") Long userId);
}