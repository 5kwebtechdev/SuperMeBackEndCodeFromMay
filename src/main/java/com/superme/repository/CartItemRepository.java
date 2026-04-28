package com.superme.repository;

import com.superme.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserIdAndSchoolIdAndClassId(Long userId, Long schoolId, Long classId);

    void deleteByUserIdAndSchoolIdAndClassId(Long userId, Long schoolId, Long classId);
}
