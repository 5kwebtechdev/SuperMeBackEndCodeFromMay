package com.superme.repository;

import com.superme.model.Like;
import com.superme.model.Tutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
    Optional<Like> findByUserIdAndTutor(Long userId, Tutor tutor);
    int countByTutor(Tutor tutor);

    boolean existsByUserIdAndTutor(Long userId, Tutor tutor);
}