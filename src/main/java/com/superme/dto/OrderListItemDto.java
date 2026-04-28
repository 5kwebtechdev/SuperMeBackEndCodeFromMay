package com.superme.dto;

import com.superme.enums.OrderStatus;
import com.superme.enums.PaymentMethod;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class OrderListItemDto {

    private Long id;                   // ORDER ID
    private String orderNumber;
    private LocalDateTime placedAt;    // "Dec 10, 10:30 AM"

    private String customerName;       // CUSTOMER
    private String customerMobile;

    private String schoolName;         // SCHOOL
    private int itemsCount;            // ITEMS: "2 items"

    private BigDecimal totalAmount;    // TOTAL: "₹850"

    private PaymentMethod paymentMethod; // PAYMENT: Paid / COD / Refunded
    private OrderStatus status;          // STATUS: New / Packed / ...

}
