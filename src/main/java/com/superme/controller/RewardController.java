package com.superme.controller;

import com.superme.exception.ResourceNotFoundException;
import com.superme.model.Reward;
import com.superme.model.User;
import com.superme.service.RewardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/rewards")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class RewardController {

    @Autowired
    private RewardService rewardService;

    /**
     * Get all available rewards.
     */
    @GetMapping("/available")
    public ResponseEntity<List<Reward>> getAvailableRewards() {
        List<Reward> rewards = rewardService.getAvailableRewards();
        if (rewards == null || rewards.isEmpty()) {
            throw new ResourceNotFoundException("No available rewards found");
        }
        return ResponseEntity.ok(rewards);
    }

    /**
     * Claim a reward using coins.
     */
    @PostMapping("/claim/{rewardId}")
    public ResponseEntity<Reward> claimReward(@PathVariable Long rewardId, @RequestParam Long userId) {
        User user = new User();
        user.setId(userId);
        Reward claimed = rewardService.claimReward(rewardId, user);
        if (claimed == null) {
            throw new ResourceNotFoundException("Reward not found or cannot be claimed");
        }
        return ResponseEntity.ok(claimed);
    }

    /**
     * Get all rewards claimed by a user.
     */
    @GetMapping("/claimed")
    public ResponseEntity<List<Reward>> getClaimedRewards(@RequestParam Long userId) {
        User user = new User();
        user.setId(userId);
        List<Reward> claimed = rewardService.getClaimedRewards(user);
        if (claimed == null || claimed.isEmpty()) {
            throw new ResourceNotFoundException("No claimed rewards found for user");
        }
        return ResponseEntity.ok(claimed);
    }
}