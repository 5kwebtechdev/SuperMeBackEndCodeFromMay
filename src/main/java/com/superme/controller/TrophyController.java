package com.superme.controller;

import com.superme.dto.TrophyResponse;
import com.superme.exception.InternalServerErrorException;
import com.superme.service.TrophyService;
import com.superme.util.UserJwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/trophies")
@RequiredArgsConstructor
public class TrophyController {

    private final TrophyService trophyService;

    /**
     * Get all user's trophies with total count using JWT authentication
     */
//    @GetMapping("/my-trophies")
//    public ResponseEntity<?> getAllUserTrophiesWithCount(@RequestHeader("Authorization") String token) {
//        try {
//            Long userId = UserJwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
//
//            // Convert Long userId to String for service methods
//            String userIdStr = userId.toString();
//
//            // Get total trophy count
//            Integer totalCount = trophyService.getUserTotalTrophyCount(userIdStr);
//
//            // Get all trophies
//            var trophies = trophyService.getUserTrophies(userIdStr)
//                    .stream()
//                    .map(TrophyResponse::fromEntity)
//                    .toList();
//
//            if (totalCount == 0 && trophies.isEmpty()) {
//                return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                        .body(Map.of(
//                                "status", HttpStatus.NOT_FOUND.value(),
//                                "message", "No trophies found for user"
//                        ));
//            }
//
//            return ResponseEntity.ok(Map.of(
//                    "totalTrophies", totalCount,
//                    "trophies", trophies
//            ));
//
//        } catch (Exception e) {
//            throw new InternalServerErrorException("Failed to retrieve trophies: " + e.getMessage());
//        }
//    }

    // solved the 404 to 204 issue when user has no trophies, and also added a log for debugging by mohit kumar
    @GetMapping("/my-trophies")
    public ResponseEntity<?> getAllUserTrophiesWithCount(
            @RequestHeader("Authorization") String token) {

        try {
            Long userId = UserJwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
            String userIdStr = userId.toString();

            Integer totalCount = trophyService.getUserTotalTrophyCount(userIdStr);

            var trophies = trophyService.getUserTrophies(userIdStr)
                    .stream()
                    .map(TrophyResponse::fromEntity)
                    .toList();

            // ✅ Return 204 instead of 404
             return ResponseEntity.ok(Map.of(
                    "totalTrophies", totalCount != null ? totalCount : 0,
                    "trophies", trophies != null ? trophies : List.of()
            ));

         } catch (Exception e) {
            throw new InternalServerErrorException("Failed to retrieve trophies: " + e.getMessage());
        }
    }
}