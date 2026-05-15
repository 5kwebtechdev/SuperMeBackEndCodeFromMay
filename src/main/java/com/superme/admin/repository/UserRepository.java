//package com.superme.admin.repository;
//
//import com.superme.model.User;
//import com.superme.enums.Role;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//import java.util.List;
//import java.util.Optional;
//
//@Repository
//public interface UserRepository extends JpaRepository<User, Long> {
//
//    Optional<User> findByEmail(String email);
//    Optional<User> findByPhone(String phone);
//    Boolean existsByEmail(String email);
//    Boolean existsByPhone(String phone);
//
//    List<User> findByRole(Role role);
//    Page<User> findByRole(Role role, Pageable pageable);
//
//    @Query("SELECT u FROM User u WHERE u.role = :role AND " +
//           "(LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
//           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
//           "LOWER(u.phone) LIKE LOWER(CONCAT('%', :search, '%')))")
//    Page<User> searchByRoleAndKeyword(@Param("role") Role role,
//                                       @Param("search") String search,
//                                       Pageable pageable);
//
//    @Query("SELECT u FROM User u WHERE u.role = :role AND u.enabled = :enabled")
//    List<User> findByRoleAndEnabled(@Param("role") Role role, @Param("enabled") boolean enabled);
//}