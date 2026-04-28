package com.superme.controller;

import com.superme.dto.MoodDisplayResponse;
import com.superme.dto.MoodTransitionDto;
import com.superme.exception.*;
import com.superme.model.Mood;
import com.superme.model.User;
import com.superme.service.MoodMetadataService;
import com.superme.service.MoodService;
import com.superme.service.UserService;
import com.superme.util.UserJwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mood")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.DELETE, RequestMethod.OPTIONS })
public class MoodController {

    private final MoodMetadataService moodMetadataService;

    @Autowired
    private UserService userService;

    public MoodController(MoodMetadataService moodMetadataService, MoodService moodService) {
        this.moodMetadataService = moodMetadataService;
        this.moodService = moodService;
    }

    /**
     * GET /mood/vibes/today
     * Returns the user's mood for today with date, mood name, and image path.
     */
    @GetMapping("/vibes/today")
    public ResponseEntity<?> getTodayVibe(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        Long userId;
        try {
            userId = Long.parseLong(principal.getName());
        } catch (NumberFormatException e) {
            throw new BusinessException("Invalid user principal");
        }
        LocalDate today = LocalDate.now();
        java.time.LocalTime time = java.time.LocalTime.MIDNIGHT;
        Mood mood = moodService.getMoodForUserByDate(userId, today, time);
        if (mood == null) {
            throw new BusinessException("No mood set for today" );
        }
        // Find image path for mood value
        String imagePath = S3_BASE_URL + "emojis/" + mood.getValue().toLowerCase() + ".png";
        return ResponseEntity.ok(Map.of(
                "date", today.toString(),
                "mood", mood.getValue(),
                "imagePath", imagePath));
    }

    private static final String S3_BASE_URL = "https://your-bucket.s3.amazonaws.com/";

    @Autowired
    private MoodService moodService;

    @GetMapping("/vibes")
    public List<Map<String, String>> getVibes() {
        return List.of(
                vibe("happy", "emojis/happy.png"),
                vibe("relaxed", "emojis/relaxed.png"),
                vibe("sad", "emojis/sad.png"),
                vibe("worried", "emojis/worried.png"),
                vibe("crying", "emojis/crying.png"),
                vibe("excited", "emojis/excited.png"));
    }

    private Map<String, String> vibe(String name, String path) {
        return Map.of(
                "name", name,
                "imageUrl", S3_BASE_URL + path);
    }

    /**
     * Set the user's mood for today and link it with the calendar event.
     * Expects JSON: { "vibe": "happy" }
     * Stores date/time as epoch millis.
     */
    @PostMapping("/vibes/set")
    public ResponseEntity<?> setVibe(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        try {
            // 1️⃣ Extract Authorization header
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new UnauthorizedException("Missing or invalid Authorization header");
            }
            Long userId;
            try {
                userId = UserJwtUtil.getUserIdFromToken(authHeader.substring(7));
            } catch (NumberFormatException e) {
                throw new BusinessException("Invalid user principal");
            }
            String vibe = request.get("vibe");
            if (vibe == null || vibe.isBlank()) {
                throw new BusinessException("Missing or empty 'vibe' field");
            }
            LocalDate today = LocalDate.now();
            LocalTime time = LocalTime.now();

            // 4️⃣ Check how many moods already set today
            int countToday = moodService.countMoodsByUserAndDate(userId, today);
            if (countToday >= 2) {
                throw new TooManyRequestsException("You can set your mood only 2 times per day.");
            }

            moodService.setMood(userId, today, time, vibe);
            return ResponseEntity.ok(Map.of(
                    "message", "Vibe for " + today + " set to " + vibe,
                    "date", today.toString(),
                    "vibe", vibe));
        } catch (Exception ex) {
            throw new InternalServerErrorException("Failed to set vibe: " + ex.getMessage());
       }
    }


    @GetMapping("/vibes/transition")
    public ResponseEntity<?> getTransition(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
           throw new UnauthorizedException("Missing or invalid Authorization header");
        }

        Long userId = UserJwtUtil.getUserIdFromToken(authHeader.substring(7));
        List<MoodTransitionDto> transitions = moodService.getTransition(userId);

        return ResponseEntity.ok(transitions);
    }

    @GetMapping("/vibes/transition/monthly")
    public ResponseEntity<?> getMonthlyTransitions(
            Principal principal,
            @RequestParam(required = false) Long forUserId,
            @RequestParam int year,
            @RequestParam int month) {

        User user = getUserFromPrincipal(principal);

        try {
            List<MoodTransitionDto> transitions = moodService.getMonthlyTransitions(user,forUserId, year, month);
            return ResponseEntity.ok(transitions);
        } catch (Exception ex) {
           throw new InternalServerErrorException("Failed to fetch monthly transitions: " + ex.getMessage());
        }
    }

    private User getUserFromPrincipal(Principal principal) {
        String principalName = principal.getName();
        try {
            Long userId = Long.parseLong(principalName);
            return userService.getUserById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        } catch (NumberFormatException e) {
            return userService.getUserByEmail(principalName)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        }
    }



    /**
     * Fetch mood display for the logged-in user.
     * Audience can be SELF (for own dashboard) or PARENT (for parent viewing child's activities)
     */
    @GetMapping("/vibes/display")
    public ResponseEntity<?> getMoodDisplay(@RequestHeader("Authorization") String authHeader) {
            // 1️⃣ Extract user from token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
               throw new UnauthorizedException("Missing or invalid Authorization header");
            }
            Long userId = UserJwtUtil.getUserIdFromToken(authHeader.substring(7));

            // 3️⃣ Build mood display response
            List<MoodDisplayResponse> response = moodMetadataService.buildMoodDisplayResponse(userId);

            return ResponseEntity.ok(response);
    }


}
