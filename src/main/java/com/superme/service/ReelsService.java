package com.superme.service;

import com.superme.enums.ActivityType;
import com.superme.model.Reels;
import com.superme.repository.ReelsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ReelsService {
    @Autowired
    private ReelsRepository reelsRepository;

    @Autowired
    private BadgeService badgeService;

    public Reels createReel(Reels reel) {

        Reels savedReel = reelsRepository.save(reel);

        if (savedReel.getCreatedBy() != null) {

            badgeService.triggerBadgeUpdate(
                    savedReel.getCreatedBy(),
                    ActivityType.REEL_CREATED,
                    0,              // no coins earned directly
                    "REELS"
            );
        }
        return savedReel;
    }


    public List<Reels> getAllReels() {
        return reelsRepository.findAll();
    }

    public Optional<Reels> getReelById(Long id) {
        return reelsRepository.findById(id);
    }

    public int likeReel(Long reelId, Long userId) {
        Optional<Reels> optional = reelsRepository.findById(reelId);
        if (optional.isEmpty())
            return -1;
        Reels reel = optional.get();
        boolean isNewLike = !reel.getLikedBy().contains(userId);
        reel.like(userId);
        reelsRepository.save(reel);

        // BadgeService: If the user received a new like, update badge progress for reel owner
        // 🔔 Trigger badge evaluation ONLY on new like
        if (isNewLike && reel.getCreatedBy() != null) {
            badgeService.triggerBadgeUpdate(
                    reel.getCreatedBy(),
                    ActivityType.REEL_LIKED,
                    0,
                    "REELS"
            );
        }

        return reel.getLikedBy().size();
    }

    public void saveReel(Long reelId, Long userId) {
        Optional<Reels> optional = reelsRepository.findById(reelId);
        if (optional.isEmpty())
            return;
        Reels reel = optional.get();
        reel.save(userId);
        reelsRepository.save(reel);
    }

    public int shareReel(Long reelId) {
        Optional<Reels> optional = reelsRepository.findById(reelId);
        if (optional.isEmpty())
            return -1;
        Reels reel = optional.get();
        reel.share();
        reelsRepository.save(reel);
        return reel.getShareCount();
    }

    // Helper for Social Star badge: get total likes across all reels for a user
    private int getTotalLikesByUser(Long userId) {
        List<Reels> userReels = reelsRepository.findReelsByUser(userId);
        return userReels.stream().mapToInt(r -> r.getLikedBy().size()).sum();
    }
}