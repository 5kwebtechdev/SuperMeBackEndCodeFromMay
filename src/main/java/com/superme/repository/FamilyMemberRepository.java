// src/main/java/com/mindfull/repository/FamilyMemberRepository.java
package com.superme.repository;

import com.superme.enums.Relationship;
import com.superme.model.Family;
import com.superme.model.FamilyMember;
import com.superme.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {
    List<FamilyMember> findByFamily(Family family);
    Optional<FamilyMember> findByUser(User user);

    // Count FamilyMembers by User.Relationship type
    default long countByRelationship(Family family, Relationship relationship) {
        return findByFamily(family).stream()
            .filter(fm -> fm.getUser() != null && fm.getUser().getRelationship() == relationship)
            .count();
    }

    default long countChildren(Family family) {
        return countByRelationship(family, Relationship.CHILD);
    }

    default long countParents(Family family) {
        return countByRelationship(family, Relationship.PARENT);
    }

    Optional<FamilyMember> findByUserAndFamilyId(User child, Long id);
}