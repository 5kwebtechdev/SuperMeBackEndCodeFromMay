package com.superme.repository;

import com.superme.model.ChallengeOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChallengeOptionRepository extends JpaRepository<ChallengeOption, Long> {
    List<ChallengeOption> findByChallengeId(Long challengeId);
}