package com.superme.controller;

import com.superme.dto.BadgePopupDTO;
import com.superme.dto.BadgeText;
import com.superme.exception.BusinessException;
import com.superme.service.BadgeService;
import com.superme.util.UserJwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/badges")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class BadgeController {

    @Autowired
    private BadgeService badgeService;

    /**
     * Get all badges for the authenticated user with progress (for badge page, DTO-based)
     */
//    @GetMapping("/progress")
//    public ResponseEntity<?> getUserBadgesWithProgress(@RequestHeader("Authorization") String token) {
//        Long userId = UserJwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
//        BadgeText badgeText = badgeService.getBadgesForUser(userId);
//
//        if (badgeText.getBadges() == null || badgeText.getBadges().isEmpty()) {
//            throw new BusinessException("No badges found for user");
//        }
//
//        // Calculate total badges earned
//        int totalEarned = (int) badgeText.getBadges().stream()
//                .filter(badge -> Boolean.TRUE.equals(badge.getIsEarned()))
//                .count();
//
//        // Set the calculated value
//        badgeText.setTotalBadgesEarned(totalEarned);
//
//        return ResponseEntity.ok(badgeText);
//    }


    // handled 404 to 204 by mohit kumar
    @GetMapping("/progress")
    public ResponseEntity<?> getUserBadgesWithProgress(
            @RequestHeader("Authorization") String token) {

        Long userId = UserJwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        BadgeText badgeText = badgeService.getBadgesForUser(userId);

        // ✅ Return 204 if no badges
        if (badgeText.getBadges() == null || badgeText.getBadges().isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        // Calculate total badges earned
        int totalEarned = (int) badgeText.getBadges().stream()
                .filter(badge -> Boolean.TRUE.equals(badge.getIsEarned()))
                .count();

        badgeText.setTotalBadgesEarned(totalEarned);

        return ResponseEntity.ok(badgeText);
    }

    /**
     * Get all badges earned by the authenticated user as DTOs (only earned ones)
     */
    @GetMapping("/earned")
    public ResponseEntity<?> getBadgesEarnedByUser(@RequestHeader("Authorization") String token) {
        Long userId = UserJwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        BadgeText badgeText = badgeService.getBadgesForUser(userId);

        if (badgeText.getBadges() == null || badgeText.getBadges().isEmpty()) {
            throw new BusinessException("No badges found for user");
        }

        // Filter only earned badges
        var earnedBadges = badgeText.getBadges().stream()
                .filter(badge -> Boolean.TRUE.equals(badge.getIsEarned()))
                .toList();

        if (earnedBadges.isEmpty()) {
            throw new BusinessException("No earned badges found for user");
        }

        // Return earned badges with count
        return ResponseEntity.ok(Map.of(
                "totalBadgesEarned", earnedBadges.size(),
                "earnedBadges", earnedBadges
        ));
    }

    /**
     * Get only the count of earned badges (lightweight endpoint)
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getBadgeSummary(@RequestHeader("Authorization") String token) {
        Long userId = UserJwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        BadgeText badgeText = badgeService.getBadgesForUser(userId);

        if (badgeText.getBadges() == null || badgeText.getBadges().isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "totalBadgesEarned", 0
            ));
        }

        // Calculate total badges earned
        int totalEarned = (int) badgeText.getBadges().stream()
                .filter(badge -> Boolean.TRUE.equals(badge.getIsEarned()))
                .count();

        return ResponseEntity.ok(Map.of(
                "totalBadgesEarned", totalEarned
        ));
    }

    /**
     * 🎉 Get pending badge popup (shown once)
     * Frontend should call this after any user action or screen load
     */
    @GetMapping("/popup/pending")
    public ResponseEntity<BadgePopupDTO> getBadgePopup(@RequestHeader("Authorization") String token) {
        Long userId = UserJwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        BadgePopupDTO popup = badgeService.getPendingBadgePopup(userId);
        return ResponseEntity.ok(popup);
    }

    /**
     * ✅ Acknowledge popup (mark as shown)
     * Called when user closes the popup or taps CTA
     */
    @PostMapping("/popup/{badgeId}/ack")
    public ResponseEntity<Void> acknowledgePopup(@RequestHeader("Authorization") String token,@PathVariable Long badgeId) {
        Long userId = UserJwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        badgeService.acknowledgeBadgePopup(badgeId,userId);
        return ResponseEntity.ok().build();
    }
}