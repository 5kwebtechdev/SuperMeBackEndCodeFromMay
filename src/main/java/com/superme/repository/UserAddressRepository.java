package com.superme.repository;

import com.superme.model.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    List<UserAddress> findByUserIdOrderByIsDefaultDescIdDesc(Long userId);

    boolean existsByUserId(Long userId);

    Optional<UserAddress> findByIdAndUserId(Long id, Long userId);

    Optional<UserAddress> findFirstByUserIdOrderByIsDefaultDescIdDesc(Long userId);

    @Modifying
    @Query("update UserAddress a set a.isDefault = false where a.userId = :userId")
    void clearDefaultForUser(Long userId);
}
