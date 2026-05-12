package com.superme.repository;

import com.superme.model.AccountDeleteRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AccountDeleteRequestRepository extends JpaRepository<AccountDeleteRequest, Long> {
    List<AccountDeleteRequest> findByUserIdAndStatus(Long userId, String status);
    boolean existsByUserIdAndStatus(Long userId, String status);
}