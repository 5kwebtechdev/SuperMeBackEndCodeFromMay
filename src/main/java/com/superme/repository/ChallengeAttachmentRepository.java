package com.superme.repository;

import com.superme.model.ChallengeAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChallengeAttachmentRepository extends JpaRepository<ChallengeAttachment, Long> {
    List<ChallengeAttachment> findByChallengeId(Long challengeId);

    void deleteByChallengeId(Long challengeId);
}
