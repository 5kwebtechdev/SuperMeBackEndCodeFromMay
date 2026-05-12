package com.superme.service;

import com.superme.dto.AccountDeleteRequestDTO;
import com.superme.dto.AccountDeleteResponseDTO;
import com.superme.exception.BusinessException;
import com.superme.exception.UnauthorizedException;
import com.superme.model.AccountDeleteRequest;
import com.superme.model.User;
import com.superme.repository.AccountDeleteRequestRepository;
import com.superme.repository.UserRepository;
import com.superme.util.UserJwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AccountDeletionService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountDeleteRequestRepository deleteRequestRepository;



    @Transactional
    public AccountDeleteResponseDTO processDeleteRequest(AccountDeleteRequestDTO request, String authHeader) {
        
        // 1. Verify token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing or invalid Authorization header");
        }
        
        String token = authHeader.substring(7);
        
        // 2. Extract user_id from token (this is the authenticated user)
        Long tokenUserId = UserJwtUtil.getUserIdFromToken(token);
        
        // 3. Validate that the token user_id matches the payload user_id
        if (request.getUser_id() == null || !request.getUser_id().equals(tokenUserId)) {
            throw new BusinessException("User ID mismatch. You can only request deletion for your own account.");
        }
        
        // 4. Get user from database
        User user = userRepository.findById(tokenUserId)
                .orElseThrow(() -> new BusinessException("User not found"));
        
        // 5. Check if user is already marked as deleted
        if (user.getDeleted() != null && user.getDeleted()) {
            throw new BusinessException("Account is already marked for deletion");
        }
        
        // 6. Check if there's already a pending request
        boolean hasPendingRequest = deleteRequestRepository.existsByUserIdAndStatus(tokenUserId, "PENDING");
        if (hasPendingRequest) {
            throw new BusinessException("You already have a pending account deletion request");
        }
        
        // 7. Validate email (optional but good practice)
        String email = request.getEmail();
        if (email == null || email.trim().isEmpty()) {
            email = user.getEmail();
        }
        
        // 8. Save delete request
        AccountDeleteRequest deleteRequest = AccountDeleteRequest.builder()
                .userId(tokenUserId)
                .email(email)
                .reason(request.getReason())
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
        
        deleteRequestRepository.save(deleteRequest);
        
        // 9. Return response
        return new AccountDeleteResponseDTO(
                true,
                "Account deletion request received. Your account and data will be deleted within 7 days."
        );
    }
}