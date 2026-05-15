package com.superme.repository;

import com.superme.model.User;
import com.superme.enums.Relationship;
import com.superme.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChildUserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    Boolean existsByEmail(String email);
    Boolean existsByPhone(String phone);
    
    // Find children by relationship
    List<User> findByRelationship(Relationship relationship);
    Page<User> findByRelationship(Relationship relationship, Pageable pageable);
    
    // Find children by family
    List<User> findByFamilyIdAndRelationship(Long familyId, Relationship relationship);
    
    // Find children by parent (through family)
    @Query("SELECT u FROM User u WHERE u.family.id = :familyId AND u.relationship = :relationship")
    List<User> findChildrenByFamilyId(@Param("familyId") Long familyId, 
                                       @Param("relationship") Relationship relationship);
    
    // Search children with pagination
    @Query("SELECT u FROM User u WHERE u.relationship = :relationship AND " +
           "(LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.phone) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchByRelationshipAndKeyword(@Param("relationship") Relationship relationship, 
                                               @Param("search") String search, 
                                               Pageable pageable);
    
    // Get active children
    @Query("SELECT u FROM User u WHERE u.relationship = :relationship AND u.enabled = true")
    List<User> findActiveChildren(@Param("relationship") Relationship relationship);
    
    // Count children in a family
    long countByFamilyIdAndRelationship(Long familyId, Relationship relationship);
}