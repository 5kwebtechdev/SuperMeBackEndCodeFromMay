package com.superme.service;

import com.superme.exception.BusinessException;
import com.superme.model.CoinTransaction;
import com.superme.model.User;
import com.superme.repository.CoinTransactionRepository;
import com.superme.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CoinService {

    @Autowired
    private CoinTransactionRepository coinTransactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BadgeService badgeService;


    public void addCoins(User user, int amount, String type, String description) {
        user.setCoins(user.getCoins() + amount);
        userRepository.save(user);

        CoinTransaction transaction = new CoinTransaction();
        transaction.setUser(user);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setType(type);
        transaction.setTimestamp(LocalDateTime.now());
        coinTransactionRepository.save(transaction);
    }

    /**
     * Spend coins for a user.
     */
    public void spendCoins(User user, int amount, String type) {
        if (user.getCoins() < amount) {
            throw new BusinessException("Insufficient coins.");
        }
        user.setCoins(user.getCoins() - amount);
        userRepository.save(user);

        CoinTransaction transaction = new CoinTransaction();
        transaction.setUser(user);
        transaction.setDescription("Spent coins");
        transaction.setAmount(-amount);
        transaction.setType(type);
        transaction.setTimestamp(LocalDateTime.now());
        coinTransactionRepository.save(transaction);

        // No badge trigger typically on spending coins
    }

    /**
     * Get coin transaction history for a user.
     */
    public List<CoinTransaction> getCoinHistory(User user) {
        return coinTransactionRepository.findByUser(user);
    }

    /**
     * Get coin balance for a user by userId.
     */
    public int getCoinBalance(Long userId) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isPresent()) {
            return userOptional.get().getCoins();
        }
        throw new BusinessException("User not found with ID: " + userId);
    }
}