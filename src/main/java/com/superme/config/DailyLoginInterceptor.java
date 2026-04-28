package com.superme.config;

import com.superme.enums.Role;
import com.superme.model.CoinTransaction;
import com.superme.model.User;
import com.superme.repository.CoinTransactionRepository;
import com.superme.repository.UserRepository;
import com.superme.util.UserJwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class DailyLoginInterceptor implements HandlerInterceptor {

    private final UserRepository userRepository;
    private final CoinTransactionRepository coinTransactionRepository;

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    @Transactional
    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return true; // unauthenticated request
        }

        Long userId;
        try {
            userId = UserJwtUtil.getUserIdFromToken(
                    authHeader.replace("Bearer ", "")
            );
        } catch (Exception e) {
            return true; // invalid token → let controller handle auth
        }

        User user = userRepository.findByIdForUpdate(userId).orElse(null);
        if (user == null || user.getRole() != Role.USER) {
            return true;
        }

        LocalDate today = LocalDate.now(ZONE);

        // ✅ Already rewarded today
        if (today.equals(user.getLastDailyBonusDate())) {
            return true;
        }

        // ⭐ NEW: count unique active day
        updateTotalActiveDays(user);

        // 1️⃣ Update LOGIN streak
        updateLoginStreak(user, today);

        // 2️⃣ Calculate bonus (5 daily + 10 on every 7th day)
        int bonusCoins = calculateDailyBonus(user.getCurrentStreak());

        // 3️⃣ Award coins
        user.setCoins(user.getCoins() + bonusCoins);
        user.setLastDailyBonusDate(today);

        CoinTransaction tx = new CoinTransaction();
        tx.setUser(user);
        tx.setAmount(bonusCoins);
        tx.setType("DAILY_LOGIN_BONUS");

        if (user.getCurrentStreak() % 7 == 0) {
            tx.setDescription("Daily bonus + 7-day streak bonus");
        } else {
            tx.setDescription("Daily login bonus");
        }

        tx.setTimestamp(LocalDateTime.now(ZONE));
        coinTransactionRepository.save(tx);

        userRepository.save(user);
        return true;
    }

    // ---------------- helpers ----------------

    private void updateLoginStreak(User user, LocalDate today) {

        LocalDate lastStreakDate = user.getLastStreakDate();

        // Already counted today → do nothing
        if (today.equals(lastStreakDate)) {
            return;
        }

        if (lastStreakDate == null) {
            user.setCurrentStreak(1);
        }
        else if (lastStreakDate.plusDays(1).equals(today)) {
            user.setCurrentStreak(user.getCurrentStreak() + 1);
        }
        else {
            user.setLastMissedStreakDate(lastStreakDate);
            user.setLastStreakCount(user.getCurrentStreak());
            user.setCurrentStreak(1);
        }

        if (user.getCurrentStreak() > user.getHighestStreak()) {
            user.setHighestStreak(user.getCurrentStreak());
        }

        // 🔑 update ONLY streak date
        user.setLastStreakDate(today);
    }


    private int calculateDailyBonus(int currentStreak) {
        int bonus = 5; // daily bonus

        // Extra streak bonus on every 7th day
        if (currentStreak > 0 && currentStreak % 7 == 0) {
            bonus += 10;
        }

        return bonus;
    }

    private void updateTotalActiveDays(User user) {
        if (user.getTotalActiveDays() == null) {
            user.setTotalActiveDays(1);
        } else {
            user.setTotalActiveDays(user.getTotalActiveDays() + 1);
        }
    }


}


