package com.superme.service;

import com.superme.enums.ActivityType;
import com.superme.enums.Relationship;
import com.superme.model.User;
import com.superme.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PetService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BadgeService badgeService;

    public String enhancePet(Long userId, int coinsToSpend) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRelationship() != Relationship.CHILD) {
            return "Only kids can enhance pets!";
        }

        if (user.getCoins() < coinsToSpend) {
            return "Not enough coins!";
        }

        // 1️⃣ Deduct coins
        user.setCoins(user.getCoins() - coinsToSpend);
        userRepository.save(user);

        // 2️⃣ Trigger badge evaluation (SINGLE CALL)
        badgeService.triggerBadgeUpdate(
                user.getId(),
                ActivityType.PET_ENHANCED,
                0,          // coins spent, not earned
                "PETS"
        );

        return "Pet enhanced successfully!";
    }

}