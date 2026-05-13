package com.superme.repository;

import com.superme.model.Avatar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AvatarRepository extends JpaRepository<Avatar, Long> {
    Optional<Avatar> findByAvatarName(String avatarName);
    Optional<Avatar> findByAvatarImageName(String avatarImageName);
    boolean existsByAvatarNameIgnoreCase(String avatarName);
}