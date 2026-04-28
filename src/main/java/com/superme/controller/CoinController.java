package com.superme.controller;

import com.superme.exception.BusinessException;
import com.superme.model.CoinTransaction;
import com.superme.model.User;
import com.superme.repository.UserRepository;
import com.superme.service.CoinService;
import com.superme.util.UserJwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/coins")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.DELETE, RequestMethod.OPTIONS })
public class CoinController {

    @Autowired
    private CoinService coinService;

    @Autowired
    private UserRepository userRepository;
    /**
     * Get the coin balance of a user.
     */
    @GetMapping("/balance")
    public ResponseEntity<?> getCoinBalance(@RequestHeader("Authorization") String token) {
        try {
            Long userId = UserJwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
            Integer balance = coinService.getCoinBalance(userId);
            return ResponseEntity.ok(Map.of(
//                    "status", HttpStatus.OK.value(),
                    "balance", balance
            ));
        } catch (Exception e) {
            throw new BusinessException("Error retrieving coin balance: " + e.getMessage());
        }
    }

    /**
     * Get the coin transaction history of a user.
     */
    @GetMapping("/history")
    public ResponseEntity<?> getCoinHistory(@RequestHeader("Authorization") String token) {
        try {
            Long userId = UserJwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
            User user = new User();
            user.setId(userId);
            List<CoinTransaction> history = coinService.getCoinHistory(user);
            return ResponseEntity.ok(Map.of(
                    "history", history,
                    "totalCoins", coinService.getCoinBalance(userId)
            ));
        } catch (Exception e) {
            throw new BusinessException("Error retrieving coin history: " + e.getMessage());
        }
    }

    /**
     * Spend coins for a specific purpose.
     */
    @PostMapping("/spend")
    public ResponseEntity<?> spendCoins(@RequestHeader("Authorization") String token,
                                        @RequestParam int amount,
                                        @RequestParam String type) {
        try {
            Long userId = UserJwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
            User user = new User();
            System.out.println("user.getCoins =  "+user.getCoins());
            user.setId(userId);
            coinService.spendCoins(user, amount, type);
            return ResponseEntity.ok(Map.of(
                    "status", HttpStatus.OK.value(),
                    "message", "Coins spent successfully"
            ));
        } catch (Exception e) {
            throw new BusinessException(e.getMessage());
        }
    }

    /**
     * Add coins to a user's account.
     */
    @PostMapping("/add")
    public ResponseEntity<?> addCoins(@RequestHeader("Authorization") String token,
                                      @RequestParam int amount,
                                      @RequestParam String type,
                                      @RequestParam String description) {

        Long userId = UserJwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        coinService.addCoins(user, amount, type, description);

        return ResponseEntity.ok(Map.of(
                "status", HttpStatus.OK.value(),
                "message", "Coins added successfully"
        ));
    }
}