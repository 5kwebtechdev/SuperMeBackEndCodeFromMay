package com.superme.repository;

import com.superme.model.Order;
import com.superme.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    Page<Order> findByOrderNumberContainingIgnoreCaseOrCustomerNameContainingIgnoreCaseOrCustomerMobileContaining(
            String orderNumber,
            String customerName,
            String customerMobile,
            Pageable pageable
    );
}
