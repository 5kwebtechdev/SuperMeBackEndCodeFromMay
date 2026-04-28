package com.superme.repository;

import com.superme.model.Reward;
import com.superme.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RewardRepository extends JpaRepository<Reward, Long> {

    // Find all rewards that are not claimed
    List<Reward> findByIsClaimedFalse();

    // Find all rewards claimed by a specific user
    List<Reward> findByUser(User user);
}