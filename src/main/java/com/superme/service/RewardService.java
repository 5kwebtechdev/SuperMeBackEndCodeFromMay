package com.superme.service;

import com.superme.exception.BusinessException;
import com.superme.exception.ResourceNotFoundException;
import com.superme.model.Reward;
import com.superme.model.User;
import com.superme.repository.RewardRepository;
import com.superme.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RewardService {

    @Autowired
    private RewardRepository rewardRepository;

    @Autowired
    private CoinService coinService;

    /**
     * Get available rewards.
     */
    public List<Reward> getAvailableRewards() {
        return rewardRepository.findByIsClaimedFalse();
    }

    /**
     * Claim a reward.
     */
//    public Reward claimReward(Long rewardId, User user) {
//        Reward reward = rewardRepository.findById(rewardId)
//                .orElseThrow(() -> new ResourceNotFoundException("Reward not found."));
//        if (reward.isClaimed()) {
//            throw new BusinessException("Reward already claimed.");
//        }
//        if (user.getCoins() < reward.getCost()) {
//            throw new BusinessException("Insufficient coins.");
//        }
//
//        // Deduct coins and mark reward as claimed
//        coinService.spendCoins(user, reward.getCost(), "REWARD_REDEMPTION");
//        reward.setUser(user);
//        reward.setClaimedAt(LocalDateTime.now());
//        reward.setClaimed(true);
//        return rewardRepository.save(reward);
//    }



    @Autowired
    private UserRepository userRepository;

    public Reward claimReward(Long rewardId, User userInput) {

        User user = userRepository.findById(userInput.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Reward reward = rewardRepository.findById(rewardId)
                .orElseThrow(() -> new ResourceNotFoundException("Reward not found."));

        if (reward.isClaimed()) {
            throw new BusinessException("Reward already claimed.");
        }

        if (user.getCoins() < reward.getCost()) {
            throw new BusinessException("Insufficient coins.");
        }

        coinService.spendCoins(user, reward.getCost(), "REWARD_REDEMPTION");

        reward.setUser(user);
        reward.setClaimedAt(LocalDateTime.now());
        reward.setClaimed(true);

        return rewardRepository.save(reward);
    }






    /**
     * Get rewards claimed by a user.
     */
    public List<Reward> getClaimedRewards(User user) {
        return rewardRepository.findByUser(user);
    }
}