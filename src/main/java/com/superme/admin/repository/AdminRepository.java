package com.superme.admin.repository;

import com.superme.admin.model.Admin;
import com.superme.enums.Role;
import com.superme.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Admin entity.
 * Provides CRUD operations and custom query methods for Admins.
 */
@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {
    boolean existsByRole(Role role);
    /**
     * Finds an Admin by their email address.
     * @param email the email of the admin
     * @return the Admin entity if found, otherwise null
     */
    Optional<Admin> findByEmail(String email);

    /**
     * Deletes an Admin by their ID.
     * Note: JpaRepository already provides deleteById(Long id) method,
     * but this is a custom implementation if needed.
     * @param id the ID of the admin to delete
     */
    @Modifying
    @Transactional
    void deleteById(Long id);

    Optional<Admin> findByPhone(String phone);

    /**
     * Finds an active Admin by their ID.
     * @param id the ID of the admin
     * @return the Admin entity if found and active, otherwise empty
     */

    List<Admin> findByRole(Role role);

}
