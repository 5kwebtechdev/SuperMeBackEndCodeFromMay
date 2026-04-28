package com.superme.repository;

import com.superme.model.User;
import com.superme.model.UserPassword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPasswordRepository extends JpaRepository<UserPassword, Long> {
    Optional<UserPassword> findByUser(User user);

    Optional<UserPassword> findByUserId(Long userId);
}