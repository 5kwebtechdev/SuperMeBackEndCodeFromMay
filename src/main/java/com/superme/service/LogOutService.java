package com.superme.service;

import com.superme.admin.model.Admin;
import com.superme.admin.repository.AdminRepository;
import com.superme.exception.InternalServerErrorException;
import com.superme.exception.ResourceNotFoundException;
import com.superme.exception.UnauthorizedActionException;
import com.superme.model.User;
import com.superme.repository.UserRepository;
import com.superme.util.UserJwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LogOutService {




    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    public ResponseEntity<?> logout(Long userId, String token) {

        System.out.println("inside logout service, userId: " + userId);

        try {
            String jwt = token.replace("Bearer ", "");

            Long tokenUserId = null;
            boolean isAdmin = false;

            // ✅ 1️⃣ Try USER token
            try {
                tokenUserId = UserJwtUtil.getUserIdFromToken(jwt);

            } catch (Exception userEx) {


                // ✅ 2️⃣ Try ADMIN token
                try {
                    tokenUserId = UserJwtUtil.getUserIdFromAdminToken(jwt);
                    isAdmin = true;

                } catch (Exception adminEx) {

                     throw new UnauthorizedActionException("Invalid token");
                }
            }

             if (!tokenUserId.equals(userId)) {
                throw new UnauthorizedActionException("Invalid userId for this token");
            }

             if (isAdmin) {
                return ResponseEntity.ok("Admin logged out successfully");
            }

             User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            user.setLoggedIn(false);
            userRepository.save(user);

            return ResponseEntity.ok("User logged out successfully");

        } catch (UnauthorizedActionException e) {
            throw e;

        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException("User/Admin not found");

        } catch (Exception e) {
            throw new InternalServerErrorException("An error occurred while logout the account.");
        }
    }

}
