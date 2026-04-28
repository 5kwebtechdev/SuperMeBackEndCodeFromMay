package com.superme.admin.repository;

import com.superme.admin.model.AdminPassword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminPasswordRepository extends JpaRepository<AdminPassword, Long> {
  Optional<AdminPassword> findById(Long adminId);

}
